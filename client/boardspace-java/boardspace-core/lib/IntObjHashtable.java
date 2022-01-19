// IntHashtable - a Hashtable that uses ints as the keys
//
// This is 90% based on JavaSoft's java.util.Hashtable.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

package lib;

/**
 *  A Hashtable that uses ints as the keys and Objects as values.
 *  but otherwise has the same interface as standard hashtables
 * @author ddyer
 *
 */

public class IntObjHashtable<OTYPE> implements Cloneable
    {
    /// The hash table data.
    private IntHashtableEntry<OTYPE> table[];
    public OTYPE defaultValue=null;
    public void setDefaultValue(OTYPE s) { defaultValue = s; }
    /// The total number of entries in the hash table.
    private int count;
    /// Rehashes the table when count exceeds this threshold.
    private int threshold;
    /// The load factor for the hashtable.
    private float loadFactor;

    /// Constructs a new, empty hashtable with the specified initial 
    // capacity and the specified load factor.
    // @param initialCapacity the initial number of buckets
    // @param loadFactor a number between 0.0 and 1.0, it defines
    //		the threshold for rehashing the hashtable into
    //		a bigger one.
    // @exception IllegalArgumentException If the initial capacity
    // is less than or equal to zero.
    // @exception IllegalArgumentException If the load factor is
    // less than or equal to zero.
    @SuppressWarnings("unchecked")
	public IntObjHashtable( int initialCapacity, float load )
	{
	if ( initialCapacity <= 0 || load <= 0.0 )
	    throw new IllegalArgumentException();
	this.loadFactor = load;
	table = new IntHashtableEntry[initialCapacity];
	threshold = (int) ( initialCapacity * loadFactor );
	}

    /// Constructs a new, empty hashtable with the specified initial 
    // capacity.
    // @param initialCapacity the initial number of buckets
    public IntObjHashtable( int initialCapacity )
	{
	this( initialCapacity, 0.75f );
	}

    /// Constructs a new, empty hashtable. A default capacity and load factor
    // is used. Note that the hashtable will automatically grow when it gets
    // full.
    public IntObjHashtable()
	{
	this( 101, 0.75f );
	}

    /// Returns the number of elements contained in the hashtable. 
    public int size()
	{
	return count;
	}

    /// Returns true if the hashtable contains no elements.
    public boolean isEmpty()
	{
	return count == 0;
	}

    /// Returns true if the specified object is an element of the hashtable.
    // This operation is more expensive than the containsKey() method.
    // @param value the value that we are looking for
    // @exception NullPointerException If the value being searched 
    // for is equal to null.
    // @see IntHashtable#containsKey
    public synchronized boolean contains( OTYPE value )
	{
	IntHashtableEntry<OTYPE> tab[] = table;
	for ( int i = tab.length ; i-- > 0 ; )
	    {
	    for ( IntHashtableEntry<OTYPE> e = tab[i] ; e != null ; e = e.next )
		{
		if ( e.value==value ) 
		    return true;
		}
	    }
	return false;
	}

    /// Returns true if the collection contains an element for the key.
    // @param key the key that we are looking for
    // @see IntHashtable#contains
    public synchronized boolean containsKey( long key )
	{
	IntHashtableEntry<OTYPE> tab[] = table;
	int hash = (int)key;
	int index = ( hash & 0x7FFFFFFF ) % tab.length;
	for ( IntHashtableEntry<OTYPE> e = tab[index] ; e != null ; e = e.next )
	    {
	    if ( e.hash == hash && e.key == key )
		return true;
	    }
	return false;
	}

    /// Gets the object associated with the specified key in the 
    // hashtable.
    // @param key the specified key
    // @returns the element for the key or null if the key
    // 		is not defined in the hash table.
    // @see IntHashtable#put
    public synchronized OTYPE get( long key )
	{
	IntHashtableEntry<OTYPE> tab[] = table;
	int hash = (int)key;
	int index = ( hash & 0x7FFFFFFF ) % tab.length;
	for ( IntHashtableEntry<OTYPE> e = tab[index] ; e != null ; e = e.next )
	    {
	    if ( e.hash == hash && e.key == key )
		return e.value;
	    }
	return defaultValue;
	}


    /// Rehashes the content of the table into a bigger table.
    // This method is called automatically when the hashtable's
    // size exceeds the threshold.
    protected void rehash()
	{
	int oldCapacity = table.length;
	IntHashtableEntry<OTYPE> oldTable[] = table;

	int newCapacity = oldCapacity * 2 + 1;
	@SuppressWarnings("unchecked")
	IntHashtableEntry<OTYPE> newTable[] = new IntHashtableEntry[newCapacity];

	threshold = (int) ( newCapacity * loadFactor );
	table = newTable;

	for ( int i = oldCapacity ; i-- > 0 ; )
	    {
	    for ( IntHashtableEntry<OTYPE> old = oldTable[i] ; old != null ; )
		{
		IntHashtableEntry<OTYPE> e = old;
		old = old.next;

		int index = ( e.hash & 0x7FFFFFFF ) % newCapacity;
		e.next = newTable[index];
		newTable[index] = e;
		}
	    }
	}

    /// Puts the specified element into the hashtable, using the specified
    // key.  The element may be retrieved by doing a get() with the same key.
    // The key and the element cannot be null. 
    // @param key the specified key in the hashtable
    // @param value the specified element
    // @exception NullPointerException If the value of the element 
    // is equal to null.
    // @see IntHashtable#get
    // @return the old value of the key, or null if it did not have one.
    public synchronized OTYPE put( long key, OTYPE value )
	{
	// Makes sure the key is not already in the hashtable.
	IntHashtableEntry<OTYPE> tab[] = table;
	int hash = (int)key;
	int index = ( hash & 0x7FFFFFFF ) % tab.length;
	for ( IntHashtableEntry<OTYPE> e = tab[index] ; e != null ; e = e.next )
	    {
	    if ( e.hash == hash && e.key == key )
		{
		OTYPE old = e.value;
		e.value = value;
		return old;
		}
	    }

	if ( count >= threshold )
	    {
	    // Rehash the table if the threshold is exceeded.
	    rehash();
	    return put( key, value );
	    } 

	// Creates the new entry.
	IntHashtableEntry<OTYPE> e = new IntHashtableEntry<OTYPE>();
	e.hash = hash;
	e.key = key;
	e.value = value;
	e.next = tab[index];
	tab[index] = e;
	++count;
	return defaultValue;
	}


    /// Removes the element corresponding to the key. Does nothing if the
    // key is not present.
    // @param key the key that needs to be removed
    // @return the value of key, or null if the key was not found.
    public synchronized OTYPE remove( long key )
	{
	IntHashtableEntry<OTYPE> tab[] = table;
	int hash = (int)key;
	int index = ( hash & 0x7FFFFFFF ) % tab.length;
	for ( IntHashtableEntry<OTYPE> e = tab[index], prev = null ; e != null ; prev = e, e = e.next )
	    {
	    if ( e.hash == hash && e.key == key )
		{
		if ( prev != null )
		    prev.next = e.next;
		else
		    tab[index] = e.next;
		--count;
		return e.value;
		}
	    }
	return defaultValue;
	}


    /// Clears the hash table so that it has no more elements in it.
    public synchronized void clear()
	{
	IntHashtableEntry<OTYPE> tab[] = table;
	for ( int index = tab.length; --index >= 0; )
	    tab[index] = null;
	count = 0;
	}

    /// Creates a clone of the hashtable. A shallow copy is made,
    // the keys and elements themselves are NOT cloned. This is a
    // relatively expensive operation.
    @SuppressWarnings("unchecked")
	public synchronized Object clone()
	{

	    IntObjHashtable<OTYPE> t = new IntObjHashtable<OTYPE>();
	    t.table = new IntHashtableEntry[table.length];
	    for ( int i = table.length ; i-- > 0 ; )
		t.table[i] = ( table[i] != null ) ?
		    (IntHashtableEntry<OTYPE>) table[i].clone() : null;
	    return t;

	}

private static class IntHashtableEntry<OTYPE>
    {
    int hash;
    long key;
    OTYPE value;
    IntHashtableEntry<OTYPE> next;
    @SuppressWarnings("unchecked")
	protected Object clone()
	{
	IntHashtableEntry<OTYPE> entry = new IntHashtableEntry<OTYPE>();
	entry.hash = hash;
	entry.key = key;
	entry.value = value;
	entry.next = ( next != null ) ? (IntHashtableEntry<OTYPE>) next.clone() : null;
	return entry;
	}

    }
}
