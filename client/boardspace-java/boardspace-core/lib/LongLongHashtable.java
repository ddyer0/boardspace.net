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
// IntHashtable - a Hashtable that uses ints as the keys
//
// This is 90% based on JavaSoft's java.util.Hashtable.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

package lib;
/**
 * A Hashtable that uses ints as the keys and values.
 * but otherwise has the same interface as standard hashtables
 * @author ddyer
 *
 */

public class LongLongHashtable implements Cloneable
    {
    /// The hash table data.
    private LongHashtableEntry table[];
    public int defaultValue=0;
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
    public LongLongHashtable( int initialCapacity, float load )
	{
	if ( initialCapacity <= 0 || load <= 0.0 )
	    throw new IllegalArgumentException();
	this.loadFactor = load;
	table = new LongHashtableEntry[initialCapacity];
	threshold = (int) ( initialCapacity * loadFactor );
	}

    /// Constructs a new, empty hashtable with the specified initial 
    // capacity.
    // @param initialCapacity the initial number of buckets
    public LongLongHashtable( int initialCapacity )
	{
	this( initialCapacity, 0.75f );
	}

    /// Constructs a new, empty hashtable. A default capacity and load factor
    // is used. Note that the hashtable will automatically grow when it gets
    // full.
    public LongLongHashtable()
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
    public synchronized boolean contains( long value )
	{
	LongHashtableEntry tab[] = table;
	for ( int i = tab.length ; i-- > 0 ; )
	    {
	    for ( LongHashtableEntry e = tab[i] ; e != null ; e = e.next )
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
	LongHashtableEntry tab[] = table;
	long hash = key;
	int index = (int)(( hash & 0x7FFFFFFFFFFFL ) % tab.length);
	for ( LongHashtableEntry e = tab[index] ; e != null ; e = e.next )
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
    public synchronized long get( long key )
	{
	LongHashtableEntry tab[] = table;
	long hash = key;
	int index = (int)(( hash & 0x7FFFFFFFFFFFFFL ) % tab.length);
	for ( LongHashtableEntry e = tab[index] ; e != null ; e = e.next )
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
	LongHashtableEntry oldTable[] = table;

	int newCapacity = oldCapacity * 2 + 1;
	LongHashtableEntry newTable[] = new LongHashtableEntry[newCapacity];

	threshold = (int) ( newCapacity * loadFactor );
	table = newTable;

	for ( int i = oldCapacity ; i-- > 0 ; )
	    {
	    for ( LongHashtableEntry old = oldTable[i] ; old != null ; )
		{
		LongHashtableEntry e = old;
		old = old.next;

		int index =(int)( ( e.hash & 0x7FFFFFFFFFFFFFL ) % newCapacity);
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
    public synchronized long put( long key, long value )
	{
	// Makes sure the key is not already in the hashtable.
	LongHashtableEntry tab[] = table;
	long hash = key;
	int index = (int)(( hash & 0x7FFFFFFFFFFFFFL ) % tab.length);
	for ( LongHashtableEntry e = tab[index] ; e != null ; e = e.next )
	    {
	    if ( e.hash == hash && e.key == key )
		{
		long old = e.value;
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
	LongHashtableEntry e = new LongHashtableEntry();
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
    public synchronized long remove( long key )
	{
	LongHashtableEntry tab[] = table;
	long hash = key;
	int index = (int)(( hash & 0x7FFFFFFFFFFFFFL ) % tab.length);
	for ( LongHashtableEntry e = tab[index], prev = null ; e != null ; prev = e, e = e.next )
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
	LongHashtableEntry tab[] = table;
	for ( int index = tab.length; --index >= 0; )
	    tab[index] = null;
	count = 0;
	}

    /// Creates a clone of the hashtable. A shallow copy is made,
    // the keys and elements themselves are NOT cloned. This is a
    // relatively expensive operation.
    public synchronized Object clone()
	{
	    LongLongHashtable t = new LongLongHashtable();
	    t.table = new LongHashtableEntry[table.length];
	    for ( int i = table.length ; i-- > 0 ; )
		t.table[i] = ( table[i] != null ) ?
		    (LongHashtableEntry) table[i].clone() : null;
	    return t;
	}

class LongHashtableEntry
    {
    long hash;
    long key;
    long value;
    LongHashtableEntry next;

    protected Object clone()
	{
	LongHashtableEntry entry = new LongHashtableEntry();
	entry.hash = hash;
	entry.key = key;
	entry.value = value;
	entry.next = ( next != null ) ? (LongHashtableEntry) next.clone() : null;
	return entry;
	}

    }}
