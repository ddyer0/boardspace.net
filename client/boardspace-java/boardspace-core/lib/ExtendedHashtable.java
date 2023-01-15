package lib;

import java.util.Map;
import java.util.TreeMap;

import common.GameInfo;
import online.common.OnlineConstants;
import online.common.exHashtable;

/*
 * an optionally case insensitive hashmap 
 */

public class ExtendedHashtable extends TreeMap<String,Object> implements OnlineConstants
{
	/**
	 * reworked 6/2020 to use TreeMap instead of Hashtable, so the .toLowerCase() in get(x) could be 
	 * eliminated.  It turned out this was a significant performance hit due to its use in commonMove.findUnique(x)
	 * which used it a lot, and consed a lot of lower case strings in the process. 
	 */
	static final Object NullObject = new Object();
	static final long serialVersionUID = 1L;
	private boolean noCase = false;
	  // constructor
    public ExtendedHashtable(boolean nc)
    {
        super(nc?G.CASE_INSENSITIVE_ORDER:null);
        noCase = nc;
    }

    public ExtendedHashtable()
    {
    }
  
    public void addStringPairs(Object...pairs)
    {	if(pairs!=null)
    	{
		for(int i=0;i<pairs.length;i+=2)
    	{
    		String s = (String)pairs[i];
    		int v = (int)pairs[i+1];
    		putInt(s, v);
    	}}
    }
   
    /**
     * case independent get
     * @param key
     * @return an Object
     */
        public Object get(String key)
        {
        	Object val = super.get(key);
        	if(val==NullObject) { return(null); }
            return (val);
        }
        
     private synchronized Object getInternal(String key)
        {
            return (super.get(key));
        }
     

    /**
     * case independent put
     * @param key
     * @param val
     */
        public synchronized Object put(String key, Object val)
        {	
        	super.put( key, (val==null) ? NullObject : val);
        	return(val);
        }
    /**
     * get a value expected to be an integer
     * @param key
     * @return an integer
     */
        public int getInt(String key)
        {	Object val = getInternal(key);
        	if(val instanceof Integer) { return(((Integer)val).intValue()); }
        	if(val instanceof String) { return(G.IntToken((String)val)); }
        	throw G.Error("No integer value for "+val+" for key "+key);       	
        }
    /**
     * get a value expected to be an integer, and default if not found.
     * @param key
     * @param defaultval
     * @return an integer
     */
        public int getInt(String key, int defaultval)
        {
            Object val = getInternal(key);
            if((val==null)||(val==NullObject)) { return(defaultval); }
        	if(val instanceof Integer) { return(((Integer)val).intValue()); }
        	if(val instanceof String) { return(G.IntToken((String)val)); }
        	throw G.Error("No integer value for "+val+" for key "+key);  
        }
        
        public int getInt(String key,int def,int min,int max)
        {
        	int v = getInt(key,def);
        	return(Math.max(min, Math.min(v, max)));
        }
        public double getDouble(String key,double def)
        {
        	Object val = getInternal(key);
        	if((val==NullObject)||(val==null)) { return(def); }
        	if(val instanceof Double) { return(((Double)val).doubleValue()); }
        	if(val instanceof String) { return(Double.parseDouble((String)val)); }
           	throw G.Error("No double value for "+val+" for key "+key);  
        }
    /**
     * put an integer value, error if not found.
     * @param key
     * @param v
     */
        public void putInt(String key, int v)
        {
            put(key, Integer.valueOf(v));
        }
        
        public void putInt(Enum<?> id,int v)
        {
        	put(id.name(),v);
        }
    /**
     * put an integer value {@link #putInt} is preferred.
     * @param key
     * @param v
     */
        public void put(String key, int v)
        {
            putInt(key, v);
        }
    /**
     * get a boolean value, error if not found
     * @param key
     * @return true or false
     */
        public boolean getBoolean(String key)
        {
            Object val = getInternal(key);
            if(val instanceof Boolean) { return(((Boolean)val).booleanValue());}
            if(val instanceof String) { return(Boolean.parseBoolean((String)val)); }
            throw G.Error("no boolean value for "+val+" for key "+ key);
        }
    /**
     * get a boolean value, and default if not found.
     * @param key
     * @param defaultval
     * @return a boolean
     */
        public boolean getBoolean(String key, boolean defaultval)
        {
            Object val = getInternal(key);
            if((val==null)||(val==NullObject)) { return(defaultval); }
            if(val instanceof Boolean) { return(((Boolean)val).booleanValue());}
            if(val instanceof String) { return(Boolean.parseBoolean((String)val)); }
            throw G.Error("no boolean value for "+val+" for key "+ key);
        }

    /**
     * put a boolean value
     * @param key
     * @param v
     */
        public void putBoolean(String key, boolean v)
        {
            put(key, Boolean.valueOf(v));
        }

    /**
     * get a string value, error if not found
     * @param key
     * @return a string
     */
        public String getString(String key)
        {
            Object s = getInternal(key);
            if(s==NullObject) { return(null); }
            if(s instanceof Boolean) { return(s.toString()); }
            if(s instanceof Integer) { return(s.toString()); }
            if(s instanceof String) { return((String)s); }
            throw G.Error("no value for key: " + key);
        }
        

    /**
     * 
     * @param key
     * @param defaultval
     * @return get a string value, default if not found
     */
      public String getString(String key, String defaultval)
        {
            Object s = getInternal(key);
            if((s==NullObject)||(s==null)) { return(defaultval); }
            if(s instanceof Boolean) { return(s.toString()); }
            if(s instanceof Integer) { return(s.toString()); }
            if(s instanceof String) { return((String)s); }
            throw G.Error("No string value for %s for key %s",s,key);
        }
      
      // put this the one place so we always get the same answer
      public GameInfo getGameInfo()
      {
    	  GameInfo info = (GameInfo)get(exHashtable.GAMEINFO);
          if(info==null)
          {
          	info = GameInfo.findByName(getString(GAMETYPE ,getString(GAMENAME,null)));
          	setGameInfo(info);
          }
          return info;
      }
      public void setGameInfo(GameInfo gi)
      {
    	  put(exHashtable.GAMEINFO,gi);
      }
    /**
     * put a string value, error if already present or if value is null
     * @param key
     * @param val
     */
        public void putString(String key, String val)
        {	
            if (val == null)
            {
            	throw G.Error("no value for key: " + key);
            }

            put(key, val);
        }
    /**
     * get an object value, error if not found
     * @param k
     * @return an object
     */
        public Object getObj(String k)
        {
            Object o = get(k);
            
            if (o == null)
            {
            	throw G.Error("no value for key: %s" , k);
            }
            if(o==NullObject) { return(null); }
            return (o);
        }
    /**
     * put an object value, error if already found or if object is null
     * @param k
     * @param v
     */
        public void putObj(String k, Object v)
        {
            if (v == null)
            {
            	throw G.Error("no value for key: %s" , k);
            }

            put(k, v);
        }
 
    /**
     * find a key for a value which is unique in the table.  This is
     * used to do a reverse lookup on keys whose values are integers.
     * @param someint
     * @return a String
     */
        // find a string that evaluates to someint, but insist that
        // it is unique
        public String findUnique(int someint)
        {
            String possible = null;

            for (Map.Entry<String,Object> entry : entrySet())
            {
                String k = entry.getKey();
                Object v = entry.getValue();	// k is already lower case

                if (v instanceof Integer)
                {
                    int val = ((Integer) v).intValue();

                    if (val == someint)
                    {
                        if (possible == null)
                        {
                            possible = k;
                        }
                        else
                        {
                        	throw G.Error("Multiple matches for " + someint +
                                " possibilities are " + k + " and " + possible);
                        }
                    }
                }
            }

            if (possible == null)
            {
            	throw G.Error("No Dictionary string found for %s" , someint);
            }

            return (possible);
        }
        public String findUniqueTrans(int op)
        {
        	String un = findUnique(op);
        	return G.getTranslations().get(un);
        }
    /**
     * copy an array of values from another hash table.
     * @param t
     * @param val
     */
        public void copyFrom(ExtendedHashtable t, String[] val)
        {
            for (int i = 0; i < val.length; i++)
            {
                Object v = t.get(val[i]);

                if (v != null)
                {
                    put(val[i], v);
                }
            }
        }
        public synchronized ExtendedHashtable copy()
        {	ExtendedHashtable newglobals = new ExtendedHashtable(noCase);
        	for(Map.Entry<String,Object> entry : entrySet())
        	{
        		newglobals.put(entry.getKey(),entry.getValue());
        	}
			return(newglobals);
        }
}


