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
