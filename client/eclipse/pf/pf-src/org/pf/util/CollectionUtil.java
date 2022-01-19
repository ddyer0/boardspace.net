// ===========================================================================
// CONTENT  : CLASS CollectionUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 3.1 - 19/01/2014
// HISTORY  :
//  12/03/2002  duma  CREATED
//	18/12/2002	duma	added		-->	addAll( Collection, Object[] )
//	15/01/2003	duma	added		-->	addAllNew( Collection, Object[] )
//	24/09/2003	duma	added		-->	contains( Object[], Object )
//	20/12/2003	duma	added		-->	removeNull( Object[] )
//	20/06/2004	duma	added		-->	EMPTY_SET, EMPTY_LIST, EMPTY_MAP
//	27/05/2005	mdu		added		--> copyWithout(), isNullOrEmpty(), toArray(), toList()
//	24/02/2006	mdu		changed	-->	to use IObjectFilter rather than ObjectFilter
//	21/01/2007	mdu		added		-->	contains() for long[] and int[]
//	04/04/2008	mdu		added		-->	asIterator(Enumeration), asList(Enumeration)
//	19/12/2008	mdu		added		-->	append(Object[]...)
//	13/11/2009	mdu		added		-->	append(int[]...) , append(long[]...)
//	15/01/2012	mdu		changed	-->	to generic types
//  19/01/2014  mdu   added   --> add(Map, String...)
//
// Copyright (c) 2002-2014, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.pf.bif.filter.IObjectFilter;
import org.pf.bif.text.IStringPair;

/**
 * This class implements the singleton pattern. Access the sole instance
 * with the method current().
 * Provides helpful functions on collections.
 *
 * @author Manfred Duchrow
 * @version 3.1
 */
public class CollectionUtil
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  /**
   * A constant that contains an empty object array. 
   */
  public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  /**
   * A constant that contains an empty int array. 
   */
  public static final int[] EMPTY_INT_ARRAY = new int[0];

  /**
   * A constant that contains an empty long array. 
   */
  public static final long[] EMPTY_LONG_ARRAY = new long[0];

  /**
   * A constant that contains an immutable empty Set.
   * Be aware that it is not possible to add objects to this set.
   * The clone() method will NOT return a copy, but the empty Set itself! 
   */
  public static final Set EMPTY_SET = new EmptySet();

  /**
   * A constant that contains an immutable empty List.
   * Be aware that it is not possible to add objects to this list.
   * The clone() method will NOT return a copy, but the empty List itself! 
   */
  public static final List EMPTY_LIST = new EmptyList();

  /**
   * A constant that contains an immutable empty Map.
   * Be aware that it is not possible to add objects to this map.
   * The clone() method will NOT return a copy, but the empty Map itself! 
   */
  public static final Map EMPTY_MAP = new EmptyMap();

  private static CollectionUtil singleton = new CollectionUtil();

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Returns the one and only instance of this class.
   */
  public static CollectionUtil current()
  {
    return singleton;
  } // current() 

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected CollectionUtil()
  {
    super();
  } // CollectionUtil() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Returns a new properties object with all properties that start with the given 
   * prefix copied from the specified source properties object.
   * The keys of the copied properties are cut off the prefix in the return object.
   * If source is null the method returns an empty Properties object.
   * 
   * @param source The properties to copy from
   * @param prefix The prefix all properties to copy have in common
   */
  public Properties propertiesStartingWith(Properties source, String prefix)
  {
    return this.propertiesStartingWith(source, prefix, true);
  } // propertiesStartingWith() 

  // -------------------------------------------------------------------------

  /**
   * Returns a new properties object with all properties that start with the given 
   * prefix copied from the specified source properties object.
   * The cutPrefix specifies whether or not the keys of the copied properties 
   * must be cut off the prefix in the destination object.
   * If any of the input parameters is null the method returns an empty
   * Properties object.
   * 
   * @param source The properties to copy from
   * @param prefix The prefix all properties to copy have in common
   * @param cutPrefix If true, the prefix gets cut off the keys in the destination object
   */
  public Properties propertiesStartingWith(Properties source, String prefix, boolean cutPrefix)
  {
    Properties result = new Properties();
    this.copyPropertiesStartingWith(source, result, prefix, cutPrefix);
    return result;
  } // propertiesStartingWith() 

  // -------------------------------------------------------------------------

  /**
   * Copies all properties that start with the given prefix from the
   * specified source properties object to the destination properties.
   * The cutPrefix specifies whether or not the keys of the copied properties 
   * must be cut off the prefix in the destination object.
   * 
   * @param source The properties to copy from (if null nothing happens at all)
   * @param destination The destination object where the properties are copied to (if null nothing happens at all)
   * @param prefix The prefix all properties to copy have in common (if null nothing happens at all)
   * @param cutPrefix If true, the prefix gets cut off the keys in the destination object
   */
  public void copyPropertiesStartingWith(Properties source, Properties destination, String prefix, boolean cutPrefix)
  {
    Enumeration keys = null;
    String key = null;
    String value = null;
    int indexAfterPrefix = 0;

    if ((source == null) || (destination == null) || (prefix == null))
    {
      return;
    }

    keys = source.keys();
    while (keys.hasMoreElements())
    {
      key = (String)keys.nextElement();
      if (key.startsWith(prefix))
      {
        value = source.getProperty(key);
        if (cutPrefix)
        {
          if (key.length() <= prefix.length())
          {
            continue;
          }
          indexAfterPrefix = prefix.length();
          if ((indexAfterPrefix > 0) && (!prefix.endsWith(".")))
          {
            if (key.charAt(indexAfterPrefix) == '.')
            {
              indexAfterPrefix++;
            }
          }
          key = key.substring(indexAfterPrefix);
        }
        destination.setProperty(key, value);
      }
    }
  } // copyPropertiesStartingWith() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of the given object array.
   */
  public <T> T[] copy(T... objects)
  {
    T[] copy;
    int size;

    if (objects == null)
    {
      return null;
    }
    size = objects.length;
    copy = (T[])java.lang.reflect.Array.newInstance(objects.getClass().getComponentType(), size);

    System.arraycopy(objects, 0, copy, 0, size);
    return copy;
  } // copy() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of the given object array which contains only those objects
   * that match the given filter.
   * If the given array is null, the result is null too.
   * If the given filter is null, the result is an empty array
   */
  public <T> T[] copy(T[] objects, IObjectFilter<T> filter)
  {
    return this.copy(objects, filter, false);
  } // copy() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of the given collection which contains only those objects
   * that match the given filter.
   * If the given collection is null, the result is null too.
   * If the given filter is null, the result is an empty collection.
   * If the result collection cannot be instantiated (e.g. due to missing
   * public no-argument constructor) a RuntimeException will be thrown.
   */
  public <T> Collection<T> copy(Collection<T> objects, IObjectFilter<T> filter)
  {
    return this.copyCollection(objects, filter, false);
  } // copy() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of the given map which contains only those objects
   * that match the given filter.
   * If the given map is null, the result is null too.
   * If the given filter is null, the result is an empty map.
   * The class of the given map must have a public non-argument constructor.
   * The specified filter must be able to handle Map.Entry objects. 
   * If the result map cannot be instantiated (e.g. due to missing
   * public no-argument constructor) a RuntimeException will be thrown.
   */
  public <K, V> Map<K, V> copy(Map<K, V> map, IObjectFilter<Map.Entry<K, V>> filter)
  {
    return this.copyMap(map, filter, false);
  } // copy() 

  // -------------------------------------------------------------------------

  /**
   * Returns a new array that contains all elements of the given array
   * in reverse order.
   */
  public <T> T[] reverseCopy(T[] array)
  {
    T[] copy;

    copy = this.copy(array);
    this.reverse(copy);
    return copy;
  } // reverseCopy() 

  // -------------------------------------------------------------------------

  /**
   * Puts all elements in the specified array into reverse order.
   */
  public void reverse(Object[] array)
  {
    int i, j;
    Object temp;

    if (array != null)
    {
      i = 0;
      j = array.length - 1;
      while (i < j)
      {
        temp = array[i];
        array[i] = array[j];
        array[j] = temp;
        i++;
        j--;
      }
    }
  } // reverse() 

  // -------------------------------------------------------------------------

  /**
   * Adds all given objects to the specified collection.
   * All null values in the objects array will be skipped.
   * 
   * @param collection The collection to which the objects are added
   * @param objects The objects to add to the collection
   */
  public <T> void addAll(Collection<T> collection, T[] objects)
  {
    this.addAll(collection, objects, false);
  } // addAll() 

  // -------------------------------------------------------------------------

  /**
   * Adds all given objects to the specified collection, if they are not 
   * already in the collection.
   * All null values in the objects array will be skipped.
   * 
   * @param collection The collection to which the objects are added
   * @param objects The objects to add to the collection
   */
  public <T> void addAllNew(Collection<T> collection, T[] objects)
  {
    this.addAll(collection, objects, true);
  } // addAllNew() 

  // -------------------------------------------------------------------------

  /**
   * Adds all given key/value pairs to the map.
   * Each string in the keyValuePairs array is examined for the existence 
   * of one separator ("=" or ":") and if such a separator was found,
   * split up and added (trimmed) to the map.
   * <p>
   * Example: addAll(map, "a=1", "b=2", "c=3")
   *  
   * @param map The map to which the key/value pairs must be added.
   *   If this parameter is null, nothing happens.
   * @param keyValuePairs The key/value pairs to be added.
   */
  public void addAll(Map<String, String> map, String... keyValuePairs)
  {
    IStringPair keyValuePair;

    if (map != null)
    {
      for (String string : keyValuePairs)
      {
        keyValuePair = this.splitToStringPair(string, "=", ":");
        if (keyValuePair != null)
        {
          map.put(keyValuePair.getString1(), keyValuePair.getString2());
        }
      }
    }
  } // addAll() 

  // -------------------------------------------------------------------------

  /**
   * Adds all given key/value pairs to the properties.
   * Each string in the keyValuePairs array is examined for the existence 
   * of one separator ("=" or ":") and if such a separator was found,
   * split up and added (trimmed) to the properties.
   * <p>
   * Example: addAll(properties, "a=1", "b=2", "c=3")
   *  
   * @param properties The properties to which the key/value pairs must be added.
   *   If this parameter is null, nothing happens.
   * @param keyValuePairs The key/value pairs to be added.
   */
  public void addAll(Properties properties, String... keyValuePairs)
  {
    if (properties != null)
    {
      this.addAll(new PropertiesMap(properties), keyValuePairs);
    }
  } // addAll() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the given object array extended by one element
   * that hold the specified object.
   * All elements of the array and the given object must be of the same type.
   */
  public <T> T[] append(T[] objects, T object)
  {
    T[] appObj;
    Class<T> componentType;

    if (object == null)
    {
      componentType = (Class<T>)objects.getClass().getComponentType();
    }
    else
    {
      componentType = (Class<T>)object.getClass();
    }
    appObj = (T[])Array.newInstance(componentType, 1);
    appObj[0] = object;
    return this.append(objects, appObj);
  } // append() 

  // -------------------------------------------------------------------------

  /**
   * Returns an array of objects that contains all objects given
   * by the first and second object array. The objects from the 
   * second array will be added at the end of the first array.
   * All elements of both arrays must be of the same type.
   * 
   * @param objects The array of objects to which to append
   * @param appendObjects The objects to be appended to the first array
   */
  public <T> T[] append(T[] objects, T[] appendObjects)
  {
    T[] newObjects;
    Class<T> componentType;

    if (objects == null)
    {
      return appendObjects;
    }

    if (appendObjects == null)
    {
      return objects;
    }
    componentType = (Class<T>)objects.getClass().getComponentType();

    newObjects = (T[])Array.newInstance(componentType, objects.length + appendObjects.length);
    System.arraycopy(objects, 0, newObjects, 0, objects.length);
    System.arraycopy(appendObjects, 0, newObjects, objects.length, appendObjects.length);

    return newObjects;
  } // append() 

  // -------------------------------------------------------------------------

  /**
   * Returns an array of int values that contains all ints given
   * by the first and second array. The values from the 
   * second array will be added at the end of the first array.
   * <br>
   * Null values for the arguments will be treated like empty arrays.
   * 
   * @param values The array of int values to which to append
   * @param appendValues The int values to be appended to the first array
   */
  public int[] append(int[] values, int[] appendValues)
  {
    int[] newValues = null;

    if (values == null)
    {
      values = EMPTY_INT_ARRAY;
    }

    if (appendValues == null)
    {
      appendValues = EMPTY_INT_ARRAY;
    }

    newValues = new int[values.length + appendValues.length];
    System.arraycopy(values, 0, newValues, 0, values.length);
    System.arraycopy(appendValues, 0, newValues, values.length, appendValues.length);

    return newValues;
  } // append() 

  // -------------------------------------------------------------------------

  /**
   * Returns an array of long values that contains all longs given
   * by the first and second array. The values from the 
   * second array will be added at the end of the first array.
   * 
   * @param values The array of long values to which to append
   * @param appendValues The long values to be appended to the first array
   */
  public long[] append(long[] values, long[] appendValues)
  {
    long[] newValues = null;

    if (values == null)
    {
      values = EMPTY_LONG_ARRAY;
    }

    if (appendValues == null)
    {
      appendValues = EMPTY_LONG_ARRAY;
    }

    newValues = new long[values.length + appendValues.length];
    System.arraycopy(values, 0, newValues, 0, values.length);
    System.arraycopy(appendValues, 0, newValues, values.length, appendValues.length);

    return newValues;
  } // append() 

  // -------------------------------------------------------------------------

  /**
   * Returns the index of the search object in the given object array.
   * The search object is compared to the array elements by identity check
   * (i.e. == ). The index of the first occurrence is returned!
   * <br>
   * If the object was not found the method returns -1.
   * 
   * @param objArray The array to search in
   * @param searchObj The object to search for (might be null)
   * @return The index of the first occurrence or -1 if not found
   */
  public <T> int indexOfIdentical(T[] objArray, T searchObj)
  {
    return this.indexOfObject(objArray, searchObj, true);
  } // indexOfIdentical() 

  // -------------------------------------------------------------------------

  /**
   * Returns the index of the search object in the given object array.
   * The search object is compared to the array elements by equality check
   * (i.e. equals() ). The index of the first occurrence is returned!
   * <br>
   * If the object was not found the method returns -1.
   * 
   * @param objArray The array to search in
   * @param searchObj The object to search for (might be null)
   * @return The index of the first occurrence or -1 if not found
   */
  public <T> int indexOf(T[] objArray, T searchObj)
  {
    return this.indexOfObject(objArray, searchObj, false);
  } // indexOf() 

  // -------------------------------------------------------------------------

  /**
   * Returns the index of the first object in the given object array
   * that matches the given filter.
   * <br>
   * If no matching object was not found the method returns -1.
   * 
   * @param objArray The array to search in
   * @param filter The filter to determine the object to look for (must not be null)
   * @return The index of the first occurrence or -1 if not found
   */
  public <T> int indexOf(T[] objArray, IObjectFilter<T> filter)
  {
    if (this.isNullOrEmpty(objArray))
    {
      return -1;
    }
    if (filter == null)
    {
      return -1;
    }
    for (int i = 0; i < objArray.length; i++)
    {
      if (filter.matches(objArray[i]))
      {
        return i;
      }
    }
    return -1;
  } // indexOf() 

  // -------------------------------------------------------------------------

  /**
   * Returns the index of the last object in the given object array
   * that matches the given filter.
   * <br>
   * If no matching object was not found the method returns -1.
   * 
   * @param objArray The array to search in
   * @param filter The filter to determine the object to look for (must not be null)
   * @return The index of the last occurrence or -1 if not found
   */
  public <T> int lastIndexOf(T[] objArray, IObjectFilter<T> filter)
  {
    if (this.isNullOrEmpty(objArray))
    {
      return -1;
    }
    if (filter == null)
    {
      return -1;
    }
    for (int i = objArray.length - 1; i >= 0; i--)
    {
      if (filter.matches(objArray[i]))
      {
        return i;
      }
    }
    return -1;
  } // lastIndexOf() 

  // -------------------------------------------------------------------------

  /**
   * Returns the index of the first object in the given list
   * that matches the given filter.
   * <br>
   * If no matching object was not found the method returns -1.
   * 
   * @param list The list to search in
   * @param filter The filter to determine the object to look for (must not be null)
   * @return The index of the first occurrence or -1 if not found
   */
  public <T> int indexOf(List<T> list, IObjectFilter<T> filter)
  {
    if (this.isNullOrEmpty(list))
    {
      return -1;
    }
    if (filter == null)
    {
      return -1;
    }
    for (int i = 0; i < list.size(); i++)
    {
      if (filter.matches(list.get(i)))
      {
        return i;
      }
    }
    return -1;
  } // indexOf() 

  // -------------------------------------------------------------------------

  /**
   * Returns the index of the last object in the given list
   * that matches the given filter.
   * <br>
   * If no matching object was not found the method returns -1.
   * 
   * @param list The list to search in
   * @param filter The filter to determine the object to look for (must not be null)
   * @return The index of the last occurrence or -1 if not found
   */
  public <T> int lastIndexOf(List<T> list, IObjectFilter<T> filter)
  {
    if (this.isNullOrEmpty(list))
    {
      return -1;
    }
    if (filter == null)
    {
      return -1;
    }
    for (int i = list.size() - 1; i >= 0; i--)
    {
      if (filter.matches(list.get(i)))
      {
        return i;
      }
    }
    return -1;
  } // lastIndexOf() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if the search object exists in the given object array.
   * The search object is compared to the array elements by identity check
   * (i.e. == ).
   * 
   * @param objArray The array to search in
   * @param searchObj The object to search for (might be null)
   */
  public boolean containsIdentical(Object[] objArray, Object searchObj)
  {
    return (this.indexOfIdentical(objArray, searchObj) >= 0);
  } // containsIdentical() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if the search object exists in the given object array.
   * The search object is compared to the array elements by equality check
   * (i.e. equals() ).
   * 
   * @param objArray The array to search in
   * @param searchObj The object to search for (might be null)
   */
  public boolean contains(Object[] objArray, Object searchObj)
  {
    return (this.indexOf(objArray, searchObj) >= 0);
  } // contains() 

  // -------------------------------------------------------------------------

  /**
   * Returns true only if the given value is found in the given array.
   * This works also for unsorted arrays. If you have a sorted array it is
   * better to user Arrays.binarySearch() bevause it is faster.
   * 
   * @param valueArray The array in which to look for the value
   * @param value The value to look for
   */
  public boolean contains(int[] valueArray, int value)
  {
    if (valueArray != null)
    {
      for (int i = 0; i < valueArray.length; i++)
      {
        if (valueArray[i] == value)
        {
          return true;
        }
      }
    }
    return false;
  } // contains() 

  // -------------------------------------------------------------------------

  /**
   * Returns true only if the given value is found in the given array.
   * This works also for unsorted arrays. If you have a sorted array it is
   * better to user Arrays.binarySearch() bevause it is faster.
   * 
   * @param valueArray The array in which to look for the value
   * @param value The value to look for
   */
  public boolean contains(long[] valueArray, long value)
  {
    if (valueArray != null)
    {
      for (int i = 0; i < valueArray.length; i++)
      {
        if (valueArray[i] == value)
        {
          return true;
        }
      }
    }
    return false;
  } // contains() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of the first array that contains only elements that are
   * not in the second array.   <br>
   * Objects are compared by identity (i.e. == ).
   * The return array is of the same type as the input array.
   * 
   * @param objArray The array to take the elements from
   * @param skipArray The array that contains all objects that must not be in the result array
   */
  public <T> T[] copyWithoutIdentical(T[] objArray, T[] skipArray)
  {
    ObjectCollectionFilter<T> filter;

    filter = new ObjectCollectionFilter(skipArray, true);
    return this.copyWithout(objArray, filter);
  } // copyWithoutIdentical() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of the first array that contains only elements that are
   * not in the second array.   <br>
   * Objects are compared by equality (i.e. equals() ).
   * The return array is of the same type as the input array.
   * 
   * @param objArray The array to take the elements from
   * @param skipArray The array that contains all objects that must not be in the result array
   */
  public <T> T[] copyWithout(T[] objArray, T[] skipArray)
  {
    ObjectCollectionFilter<T> filter;

    filter = new ObjectCollectionFilter(skipArray, false);
    return this.copyWithout(objArray, filter);
  } // copyWithout() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of the specified array that contains only those elements 
   * that do NOT match the given filter.    <br>
   * The return array is of the same type as the input array.
   * If the given filter is null a complete copy of the given array is returned.
   * If the given array is null then null is returned ;
   * 
   * @param objArray The array to take the elements from
   * @param filter A filter that defines all elements must not be copied into the result array
   */
  public <T> T[] copyWithout(T[] objArray, IObjectFilter<T> filter)
  {
    return this.copy(objArray, filter, true);
  } // copyWithout() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of the specified collection that contains only those elements 
   * that do NOT match the given filter.    <br>
   * The return collection is of the same type as the input collection.
   * If the given filter is null a complete copy of the given collection is returned.
   * If the given collection is null then null is returned ;
   * 
   * @param objects The collection to take the elements from
   * @param filter A filter that defines all elements must not be copied into the result collection
   */
  public <T> Collection<T> copyWithout(Collection<T> objects, IObjectFilter<T> filter)
  {
    return this.copyCollection(objects, filter, true);
  } // copyWithout() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of the specified map that contains only those elements 
   * that do NOT match the given filter.    <br>
   * The return map is of the same type as the input map.
   * If the given filter is null then a complete copy of the given map is returned.
   * If the given map is null then null is returned.
   * The class of the input map must have a public non-argument constructor,
   * otherwise a runtime exception will be thrown.
   * The given filter must handle Map.Entry objects.
   * 
   * @param map The map to take the elements from
   * @param filter A filter that defines all elements that must not be copied into the result map
   */
  public <K, V> Map<K, V> copyWithout(Map<K, V> map, IObjectFilter filter)
  {
    return this.copyMap(map, filter, true);
  } // copyWithout() 

  // -------------------------------------------------------------------------

  /**
   * Returns an iterator on the given array.
   * 
   * @param objects The objects to provide an iterator for
   */
  public <T> Iterator<T> iterator(T... objects)
  {
    return new ObjectArrayIterator<T>(objects);
  } // iterator() 

  // -------------------------------------------------------------------------

  /**
   * Removes all null values from the given array.
   * Returns a new array that contains all none null values of the 
   * input array.
   * 
   * @param array The array to be cleared of null values
   */
  public <T> T[] removeNull(T... array)
  {
    if (array == null)
    {
      return array;
    }
    return this.removeFromArray(array, null);
  } // removeNull() 

  // -------------------------------------------------------------------------

  /**
   * Returns an array which has elements of the specified element type.
   * If the collection or the element type is null then this method returns null.
   * <br>
   * This method is better than java.util.Collection.toArray() because it 
   * returns an array of the desired type (elementType[]) rather than Object[].
   * It also just creates one new array instance with the correct size. 
   * 
   * @param objects The object to be put into the result array
   * @param elementType The type of the array elements 
   * @throws ArrayStoreException if any element in the collection is not of the same type as the element type 
   */
  public <T> T[] toArray(Collection<T> objects, Class<T> elementType)
  {
    T[] resultArray;

    if ((objects == null) || (elementType == null))
    {
      return null;
    }
    resultArray = (T[])Array.newInstance(elementType, objects.size());
    objects.toArray(resultArray);
    return resultArray;
  } // toArray() 

  // -------------------------------------------------------------------------

  /**
   * Returns an which has elements of the type of the first element in the given
   * collection.
   * If the collection is null or empty then this method returns null.
   * So it is safe to do a type cast to the expected array type.
   * <br>
   * Example: Integer[] integers = (Integer[])util.toArray( intColl ) ; ) 
   * 
   * @throws ArrayStoreException if any element in the collection is not of the same type as the first element 
   */
  public <T> T[] toArray(Collection<T> objects)
  {
    if (this.isNullOrEmpty(objects))
    {
      return null;
    }
    return this.toArray(objects, (Class<T>)objects.iterator().next().getClass());
  } // toArray() 

  // -------------------------------------------------------------------------

  /**
   * Returns an ArrayList that contains all objects from the given array.
   * Null values will be skipped.
   * If the given array is null, then null is returned.
   */
  public <T> List<T> toList(T[] objects)
  {
    if (objects == null)
    {
      return null;
    }
    List list = new ArrayList(objects.length);
    this.addAll(list, objects);
    return list;
  } // toList() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if the given collection is null or has no elements.
   * 
   * @param collection The collection to check
   */
  public boolean isNullOrEmpty(Collection collection)
  {
    if (collection == null)
    {
      return true;
    }
    return collection.isEmpty();
  } // isNullOrEmpty() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if the given array is null or has no elements.
   * 
   * @param array The array to check
   */
  public boolean isNullOrEmpty(Object[] array)
  {
    if (array == null)
    {
      return true;
    }
    return array.length == 0;
  } // isNullOrEmpty() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if the given map is null or has no elements.
   * 
   * @param map The map to check
   */
  public boolean isNullOrEmpty(Map map)
  {
    if (map == null)
    {
      return true;
    }
    return map.isEmpty();
  } // isNullOrEmpty() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if the given association list is null or has no elements.
   * 
   * @param associationList The association list to check
   */
  public boolean isNullOrEmpty(AssociationList associationList)
  {
    if (associationList == null)
    {
      return true;
    }
    return associationList.isEmpty();
  } // isNullOrEmpty() 

  // -------------------------------------------------------------------------

  /**
   * Returns an iterator based on the given enumeration or null if the input
   * parameter is null, too.
   */
  public <T> Iterator<T> asIterator(Enumeration<T> enumeration)
  {
    if (enumeration == null)
    {
      return null;
    }
    return new EnumerationIterator(enumeration);
  } // asIterator() 

  // -------------------------------------------------------------------------

  /**
   * Returns a list containing all elements of the given enumeration.
   * If enumeration is null an empty list will be returned.
   * Null elements in the enumeration will be skipped.
   */
  public <T> List<T> asList(Enumeration<T> enumeration)
  {
    List list;
    Object object;

    list = new ArrayList();
    if (enumeration != null)
    {
      while (enumeration.hasMoreElements())
      {
        object = enumeration.nextElement();
        if (object != null)
        {
          list.add(object);
        }
      }
    }
    return list;
  } // asList() 

  // -------------------------------------------------------------------------

  /**
   * Convert the given collection to an Enumeration.
   */
  public <T> Enumeration<T> asEnumeration(Collection<T> collection)
  {
    return new CollectionEnumeration(collection);
  } // asEnumeration() 

  // -------------------------------------------------------------------------

  /**
   * Convert the given array to an Enumeration.
   */
  public <T> Enumeration<T> asEnumeration(T... objects)
  {
    List list;

    list = this.toList(objects);
    return this.asEnumeration(list);
  } // asEnumeration() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  /**
   * Adds all given objects to the specified collection.
   * All null values in the objects array will be skipped.
   * 
   * @param collection The collection to which the objects are added
   * @param objects The objects to add to the collection
   * @param justNew If true only new elements are added to the collection
   */
  protected void addAll(Collection collection, Object[] objects, boolean justNew)
  {
    if ((collection == null) || (objects == null))
    {
      return;
    }

    for (int i = 0; i < objects.length; i++)
    {
      if (objects[i] != null)
      {
        if (justNew)
        {
          if (!collection.contains(objects[i]))
            collection.add(objects[i]);
        }
        else
        {
          collection.add(objects[i]);
        }
      }
    }
  } // addAll() 

  // -------------------------------------------------------------------------

  protected int indexOfObject(Object[] objArray, Object searchObj, boolean identical)
  {
    if ((objArray == null) || (objArray.length == 0))
    {
      return -1;
    }

    boolean found = false;
    for (int i = 0; i < objArray.length; i++)
    {
      if (objArray[i] == null)
      {
        if (searchObj == null)
          found = true;
      }
      else
      {
        if (identical)
          found = objArray[i] == searchObj;
        else
          found = objArray[i].equals(searchObj);
      }
      if (found)
        return i;
    }
    return -1;
  } // indexOfObject() 

  // -------------------------------------------------------------------------

  /**
   * Removes the given objects from the array.
   * If removeObjects is null it means that all null values are removed from
   * the first array.
   */
  protected <T> T[] removeFromArray(T[] array, T[] removeObjects)
  {
    List<T> list;
    boolean remains;
    T[] result;

    list = new ArrayList<T>(array.length);
    for (int i = 0; i < array.length; i++)
    {
      if (removeObjects == null)
      {
        remains = array[i] != null;
      }
      else
      {
        remains = !this.contains(removeObjects, array[i]);
      }
      if (remains)
      {
        list.add(array[i]);
      }
    }
    try
    {
      result = (T[])Array.newInstance(array.getClass().getComponentType(), list.size());
    }
    catch (NegativeArraySizeException e)
    {
      // Could not happen. list.size() is always >= 0 !
      result = (T[])new Object[0];
    }
    list.toArray(result);
    return result;
  } // removeFromArray() 

  // -------------------------------------------------------------------------

  protected <T> T[] copy(T[] objArray, IObjectFilter<T> filter, boolean without)
  {
    Collection<T> result;

    if (objArray == null)
    {
      return null;
    }
    if (filter == null)
    {
      if (without)
      {
        return this.copy(objArray);
      }
      else
      {
        return (T[])java.lang.reflect.Array.newInstance(objArray.getClass().getComponentType(), 0);
      }
    }

    result = new ArrayList(objArray.length);
    for (int i = 0; i < objArray.length; i++)
    {
      if (filter.matches(objArray[i]))
      {
        if (!without)
        {
          result.add(objArray[i]);
        }
      }
      else
      {
        if (without)
        {
          result.add(objArray[i]);
        }
      }
    }
    return this.toArray(result, (Class<T>)objArray.getClass().getComponentType());
  } // copy() 

  // -------------------------------------------------------------------------

  protected Collection copyCollection(Collection objects, IObjectFilter filter, boolean without)
  {
    Collection result;
    Iterator iter;
    Object element;

    if (objects == null)
    {
      return null;
    }
    try
    {
      result = (Collection)objects.getClass().getDeclaredConstructor().newInstance();
    }
    catch (Throwable e)
    {
      throw new RuntimeException("Failed to create new result collection.", e);
    }
    if (filter == null)
    {
      if (without)
      {
        result.addAll(objects);
      }
    }
    else
    {
      for (iter = objects.iterator(); iter.hasNext();)
      {
        element = iter.next();
        if (filter.matches(element))
        {
          if (!without)
          {
            result.add(element);
          }
        }
        else
        {
          if (without)
          {
            result.add(element);
          }
        }
      }
    }
    return result;
  } // copyCollection() 

  // -------------------------------------------------------------------------

  protected Map copyMap(Map map, IObjectFilter filter, boolean without)
  {
    Map result;
    Iterator iter;
    Map.Entry entry;

    if (map == null)
    {
      return null;
    }
    try
    {
      result = (Map)map.getClass().getDeclaredConstructor().newInstance();
    }
    catch (Throwable e)
    {
      throw new RuntimeException("Failed to create new result map.", e);
    }
    if (filter == null)
    {
      if (without)
      {
        result.putAll(map);
      }
    }
    else
    {
      for (iter = map.entrySet().iterator(); iter.hasNext();)
      {
        entry = (Map.Entry)iter.next();
        if (filter.matches(entry))
        {
          if (!without)
          {
            result.put(entry.getKey(), entry.getValue());
          }
        }
        else
        {
          if (without)
          {
            result.put(entry.getKey(), entry.getValue());
          }
        }
      }
    }
    return result;
  } // copyMap() 

  // -------------------------------------------------------------------------

  protected IStringPair splitToStringPair(String string, String... separatorsToTry)
  {
    int index;
    LocalStringPair stringPair;

    for (String separator : separatorsToTry)
    {
      index = string.indexOf(separator);
      if ((index > 0) && (index < string.length() - 1)) // separator at beginning or end deliberately not allowed!
      {
        stringPair = new LocalStringPair();
        stringPair.setString1(string.substring(0, index).trim());
        stringPair.setString2(string.substring(index + separator.length()).trim());
        return stringPair;
      }
    }
    return null;
  } // splitToStringPair()

  // -------------------------------------------------------------------------
  // -------------------------------------------------------------------------
  // -------------------------------------------------------------------------

  protected static class LocalStringPair implements IStringPair
  {
    private String string1;
    private String string2;

    public String getString1()
    {
      return string1;
    } // getString1()

    public void setString1(String string)
    {
      this.string1 = string;
    } // setString1()

    public String getString2()
    {
      return string2;
    } // getString2()

    public void setString2(String string)
    {
      this.string2 = string;
    } // setString2()

    public String[] asArray()
    {
      return new String[] { this.getString1(), this.getString2() };
    } // asArray()

    public String asString(String separator)
    {
      return this.getString1() + separator + this.getString2();
    } // asString()
  }

} // class CollectionUtil 
