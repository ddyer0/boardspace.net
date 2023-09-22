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
package online.common;

import bridge.BackingStoreException;
import bridge.Preferences;
import lib.ChatInterface;
import lib.G;
import lib.InternationalStrings;
import lib.PopupManager;
import lib.Sort;
import util.PasswordCollector;

public class UserManager implements LobbyConstants
{
	private int numberOfUsers=0;
	private User[]Users = new User[0];
	private User primary = new User();
	public User primaryUser() { return(primary); }
	public int numberOfUsers() { return(numberOfUsers); }
	public User[] getUsers() { return(Users); }
	public boolean all_users_seen = false;
	
	public User addUser(int playerID,String name,String uid,boolean local)
	  { 
	    User user = playerID==-1 ? primary : new User();
	    user.isLocal = local;
	    //System.out.println("add ("+name+")");
	    user.name=name;
	    user.publicName=name;
	    user.serverIndex=playerID;
	    if(uid!=null) { user.uid=uid; }
	    if(user!=primary)
	    {int oldlen = Users.length;
	     if(numberOfUsers>=oldlen) 
	      {/* add more users */
	      User nu[]=new User[oldlen+10];
	      for(int i=0;i<oldlen;i++) { nu[i]=Users[i]; }
	      Users = nu;
	      }
	    Users[numberOfUsers]=user;
	    numberOfUsers++;	// increment later, so readers never see an unfilled slot
	    }
	    return(user);
	  }
	
	public void loadOfflineUsers()
	{
		Preferences prefs = Preferences.userRoot();
		String primaryname = prefs.get(PasswordCollector.nameKey,null);
		if(primaryname!=null) 
			{ changeOfflineUser(primaryname,false);
			}
		else {
			changeOfflineUser("me",false);
			}
		for(int i=0;i<MAX_OFFLINE_USERS;i++)
		{
			String name = prefs.get(PasswordCollector.nameKey+"-"+i,null);
			if((name!=null)&&!name.equalsIgnoreCase(primaryname))
			{	
				addUser(OFFLINE_USERID+i,name,""+(OFFLINE_USERID+i),true);
			}
		}
		
	}
/**
 * find a user by UID string
 * @param playerID
 * @return a user or null
 */
public User getExistingUser(String playerID)
{ if(playerID.equals(primary.uid)) { return(primary); }
  for(int userIndex = 0; userIndex<numberOfUsers; userIndex++)
  { User u = Users[userIndex];
    if(playerID.equals(u.uid)) { return(u); }
  }
  Bot bu =  Bot.findUid(playerID); 
  if(bu!=null) { return(bu.getUser()); }
  return(null);
}
public User getExistingUserName(String playerName)
{ if(playerName.equalsIgnoreCase(primary.publicName)) { return(primary); }
  for(int userIndex = 0; userIndex<numberOfUsers; userIndex++)
  { User u = Users[userIndex];
    if(playerName.equalsIgnoreCase(u.publicName)) { return(u); }
  }
  Bot bu =  Bot.findName(playerName); 
  if(bu!=null) { return(bu.getUser()); }
  return(null);
}
public boolean isActiveUser(User u)
{	if(u==primary) { return(true); }
	for(int i=0;i<numberOfUsers;i++) { if(Users[i]==u) { return(true); }}
	return false;
}

/**
 * find user by server channel
 * @param playerID
 * @return a user or null
 */
public User getExistingUser(int playerID)
{ if(primary.serverIndex == playerID) { return(primary); }
  for(int userIndex = 0; userIndex<numberOfUsers; userIndex++)
  { User u = Users[userIndex];
    if(u.serverIndex == playerID) { return(u); }
  }
  return(null);
}

public void markResheshComplete()
{
	all_users_seen = true;
}
public void markRefreshed()
{	all_users_seen = false;
	for (int tempInt=0;tempInt<numberOfUsers();tempInt++) 
    {
      Users[tempInt].markedInLastRefresh = false;
    }
}
public User getUserbyServerChannel(int uidn)
{	if(primary.serverIndex==uidn) { return(primary); }
	for(int userIndex = 0; userIndex<numberOfUsers; userIndex++)
	{ User u = Users[userIndex];
	if(uidn==u.serverIndex)
		{ return(u); 
		}
	}
  return(null);
}    
 

private void changeUser(Preferences prefs,String key,int userid,String newname,boolean remove)
{	
	if(remove) { prefs.remove(key);}
	else { prefs.put(key,newname); }
	User u = getExistingUser(""+userid);
	if(u!=null) 
		{ 
			if(remove) 
				{ removeUserFromUserlist(u); }
				else 
				{ u.name = u.publicName = newname;
				} 
		}
	if(!remove) { addUser(userid,newname,""+userid,true); }
	try { prefs.flush(); } catch (BackingStoreException e) 	{ };
}
public void changeOfflineUser(String newname,boolean remove)
{
	Preferences prefs = Preferences.userRoot();
	{
	int slot = -1;
	for(int i=0;i<MAX_OFFLINE_USERS;i++)
	{
		String name = prefs.get(PasswordCollector.nameKey+"-"+i,null);
		if(name!=null)
		{	if(name.equalsIgnoreCase(newname))
			{
				slot = i;
				break;
			}
			else if(slot==i) { slot = i+1; }
		}
		else if(slot==-1) { slot = i; } 
	}
	if(slot>=0)
	{	changeUser(prefs,PasswordCollector.nameKey+"-"+slot,OFFLINE_USERID+slot,newname,remove);
	}}
}

public synchronized void removeUserFromUserlist(User u)
  {
	u.dead = true;

      for(int i=0;i<numberOfUsers;i++) 
      {if(Users[i]==u)
        { numberOfUsers--;
          Users[i] = Users[numberOfUsers];	// swap the current last into position  
          Users[numberOfUsers] = null;		// make an empty slot
          break;
        }}
  }

public void FlushDeadUsers()
{ // flush the dead users at a safe point where the user list is not being held by the mouse
  if(all_users_seen)
  {
  for (int tempInt=0;tempInt<numberOfUsers;tempInt++) 
  {  User user = Users[tempInt];
    if (user.dead || (!user.markedInLastRefresh && (user.serverIndex<=ChatInterface.LASTUCHANNEL)))
      {
      //System.out.println("Timeout Removing " + user.name );
      removeUserFromUserlist(user);
      }
  }
  synchronized (this)
  {	
  // this needs to be synchronized against removeUserFromUserList
  // so the sort won't encounter nulls in the list
  Sort.sort(Users,0,numberOfUsers-1);
  }
  }
}
	
    
    // generate a menu of user names and manipulations
    public void changeUserMenu(PopupManager userMenu,boolean addOperations,exCanvas can,int slot,int ex,int ey)
    {	InternationalStrings s = G.getTranslations();
	  	userMenu.newPopupMenu(can,can.deferredEvents);
	  	userMenu.addMenuItem(s.get(EmptyName),EmptyName);
	  	for(int i=0;i<numberOfUsers;i++)
	  	{	User u = Users[i];
	  		if(u.isLocal)
	  		{
	  		userMenu.addMenuItem(u.name,u);
	  		}
	  	}
	  	if(addOperations)
	  	{
	  	userMenu.addMenuItem(s.get(AddAName),AddAName);
	  	userMenu.addMenuItem(s.get(RemoveAName),RemoveAName);
	  	}
	  	userMenu.show(ex,ey);
	  	}
 
}
