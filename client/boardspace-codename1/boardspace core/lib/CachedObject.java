package lib;

/**
 * this is a specialized cache intended for hand-drawn images, but 
 * applicable to any object that is complicated or expensive to create
 * and needs to be cached.  This is intended for reasonable light use,
 * where only a small number of instances will be cached.
 * 
 * Creation of this class was precipitated by the discovery that 
 * G.drawAACircle falls into a dangerous zone in codename1, because
 * it can cause drawing to overrun the drawing buffer.
 * See issue  #3388
 */
 
public class CachedObject<TYPE>
{
	CachedObject<TYPE> instances[] = null;
	TYPE tile = null;
	Object parameters[];
	long usedAt = 0;
	
	/**
	 * create a new object cache, which will cache objects
	 * @param size
	 */
	@SuppressWarnings("unchecked")
	public CachedObject(int size) { instances = new CachedObject[size]; }
	
	/** create an instance of the cache for a particular object */
	public CachedObject(TYPE im,Object ...params)
	{	tile = im;
		parameters = params;
		usedAt = G.Date();
	}
	/**
	 * look in the cache for a particular instance
	 * @param parameters
	 * @return
	 */
	public TYPE find(Object ...parameters)
	{	
		for(CachedObject<TYPE> tile : instances)
		{	
			if(tile == null) { return(null); }
			Object tp[] = tile.parameters;
			boolean match = parameters.length == tp.length;
			for(int i=0;match && i<parameters.length;i++)
					{ match &= tp[i].equals(parameters[i]);
					}
			if(match) {
				return(tile.tile); 
				}
		}
		return(null);
	}
	/**
	 * add a new object to the cache.  If it's full, replace the least
	 * recently used item
	 * @param newtile
	 */
	public void add(CachedObject<TYPE> newtile)
	{	CachedObject<TYPE> oldest = instances[0];
		int oldindex = 0;
		for(int i=0;i<instances.length;i++)
		{
			CachedObject<TYPE> t = instances[i];
			if(t==null) { instances[i] = newtile; return; }
			if(t.usedAt<oldest.usedAt) { oldest = t; oldindex = i; }
		}
		instances[oldindex] = newtile;
	}
}
