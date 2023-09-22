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

public class Preferences {
	public static Preferences userRoot() { return(new Preferences()); }

	public String[] keys()
	{	// not implemented.  Currently needed only to prune saved games.
		return(new String[0]);
  	}
	
  		
	public void put(String namekey, String name) 
	{
		com.codename1.io.Preferences.set(namekey,name);
	}
	public String get(String name,String def) 
	{
		return(com.codename1.io.Preferences.get(name,def));
	}
	//
	// Note, don't implement these because standard java doesn't.
	//
	//public void put(String namekey,boolean name)
	//{
	//	com.codename1.io.Preferences.set(namekey,name);
	//}
	//public boolean getBoolean(String name,boolean def)
	//{
	//	return(com.codename1.io.Preferences.get(name,def));
	//}
	public void flush() throws BackingStoreException
	{
		
	}
	public void remove(String key) {
		com.codename1.io.Preferences.delete(key);		
	}
}
