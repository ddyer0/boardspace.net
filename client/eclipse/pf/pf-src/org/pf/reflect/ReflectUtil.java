// ===========================================================================
// CONTENT  : CLASS ReflectUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.1 - 12/08/2012
// HISTORY  :
//  27/09/2002  duma  CREATED
//	24/10/2002	duma	added		-> isDefaultVisibility()
//	03/07/2004	mdu		bugfix	-> findMethod() now checks isAssignable() for all argument types
//	03/06/2006	mdu		changed	-> visibility of constructor to protected
//	25/02/2007	mdu		added		-> hasPublicMethod()
//	16/11/2007	mdu		changed	-> findClass()
//	21/03/2008	mdu		added		-> class loader based instance
//	19/12/2008	mdu		added		-> getInstancesOf(), implementsInterface(), indexOf()
//	13/11/2009	mdu		added		-> toArray()
//	15/01/2012	mdu		added 	-> getAnnotationValueFrom()
//	12/08/2012	mdu		added 	-> getAllTypesOf()
//
// Copyright (c) 2002-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.reflect ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The sole instance (Singleton) of this class can be accessed by method
 * <em>current()</em>.  <p>
 * It provides convenience methods on top of the normal standard Java
 * reflection API. However, it allows access to fields, methods and constructors
 * regardless of their visibility (i.e. private, protected, default, public).
 * Of course ignoring the visibility is only possible in environments that
 * have not Java 2 Security turned on. With a security manager present such
 * access to normally invisible members will cause an exception. 
 *
 * @author Manfred Duchrow
 * @version 2.1
 */
public class ReflectUtil
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/**
	 *  A reusable empty array of type Class[]
	 */
	public static final Class[] EMPTY_CLASS_ARRAY = new Class[0] ;

	/**
	 *  A reusable empty array of type Field[]
	 */
	public static final Field[] EMPTY_FIELD_ARRAY = new Field[0] ;
	
	/**
	 *  A reusable empty array of type Method[]
	 */
	public static final Method[] EMPTY_METHOD_ARRAY = new Method[0] ;
	
	/**
	 *  A reusable empty array of type Constructor[]
	 */
	public static final Constructor[] EMPTY_CONSTRUCTOR_ARRAY = new Constructor[0] ;
	
	private static final int NOT_FOUND = -1 ;

  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static ReflectUtil currentInstance = new ReflectUtil() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private ClassLoader loader = null ;

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Returns an instance this class supports. It uses this calls' class loader.
   */
  public static ReflectUtil current()
  {
    return currentInstance ;
  } // current() 
 
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected ReflectUtil()
  {
    super() ;
  } // ReflectUtil() 
  
  // -------------------------------------------------------------------------
 
  /**
   * Initialize the new instance with a different class loader.
   * 
   * @param classLoader The class loader to be used to load classes by name.
   */
  public ReflectUtil( ClassLoader classLoader )
  {
  	super() ;
  	this.loader = classLoader ;
  } // ReflectUtil() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true, if the class with the given name can be found in the
   * classpath.
   */
	public boolean classExists( String className )
	{
		return ( this.findClass( className ) != null ) ;
	} // classExists() 
 
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the classes corresponding to the given (full qualified) class names.
	 * 
	 * @param classNames The full qualified names of the classes to look for.
	 * @return An array with the same number of elements as the input parameter containing
	 * the class object corresponding to the given class names.
	 * @throws ClassNotFoundException If any of the classes cannot be found.
	 */
	public Class[] findClasses(String... classNames) throws ClassNotFoundException 
	{
		Class[] classes;
		Class clazz;

		if (classNames == null)
		{
			return new Class[0];
		}
		classes = new Class[classNames.length];
		for (int i = 0; i < classNames.length; i++)
		{
			clazz = this.findClass(classNames[i]);
			if (clazz == null)
			{
				throw new ClassNotFoundException(classNames[i]);
			}
			classes[i] = clazz;
		}
		return classes;
	} // findClasses()
	
	// -------------------------------------------------------------------------

	/**
	 * Returns the class object corresponding to the given class name or null,
	 * if the class can't be found. For primitive types the names "boolean",
	 * "int", "float" and so on can be used. The corresponding Boolean.TYPE,
	 * Integer.TYPE, Float.TYPE and so on will be returned.
	 * 
	 * @param className The full qualified name of the class
	 */
	public Class findClass( String className )
	{
		Class clazz = null ;
		
		if ( className == null )
		{
			return null ;
		}
		if ( className.equals( "boolean" ) )
    {
      return Boolean.TYPE;
    }
		if ( className.equals( "int" ) )
		{
		  return Integer.TYPE;
		}
		if ( className.equals( "long" ) )
		{
		  return Long.TYPE;
		}
		if ( className.equals( "char" ) )
		{
		  return Character.TYPE;
		}
		if ( className.equals( "byte" ) )
		{
		  return Byte.TYPE;
		}
		if ( className.equals( "short" ) )
		{
		  return Short.TYPE;
		}
		if ( className.equals( "float" ) )
		{
		  return Float.TYPE;
		}
		if ( className.equals( "double" ) )
		{
		  return Double.TYPE;
		}
			
		try
		{
			clazz = this.getLoader().loadClass( className ) ;
		}
		catch (ClassNotFoundException e)
		{
			return null;
		}
		return clazz ;
	} // findClass() 
 
	// -------------------------------------------------------------------------

	/**
	 * Tries to create an instance of the class with the given name. It uses the
	 * given caller to find the class (via forName()) to ensure that the correct
	 * classloader is used. If the caller is null it uses this class for the lookup.
	 * 
	 * @param className The name of the class to instantiate.
	 * @param caller The object which class will be used to search for the className.
	 * @param params 0-n parameters that define the constructor to be used.
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public Object createInstanceOf(String className, Object caller, Object...params) 
	{
		return this.createInstanceOfType(Object.class, className, caller, params);
	} // createInstanceOf() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Tries to create an instance of the class with the given name. It uses the
	 * given caller to find the class (via forName()) to ensure that the correct
	 * classloader is used. If the caller is null it uses this class for the lookup.
	 * 
	 * @param type The expected type of the instance (could be an interface or also java.lang.Object) 
	 * @param className The name of the class to instantiate.
	 * @param caller The object which class will be used to search for the className.
	 * @param params 0-n parameters that define the constructor to be used.
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public <T> T createInstanceOfType(Class<T> type, String className, Object caller, Object...params) 
	{
		Class callerClass;
		Class<T> aClass;
		
		if (className == null)
		{
			return null;
		}
		if (caller == null)
		{
			callerClass = this.getClass();
		}
		else
		{
			callerClass = caller.getClass();
		}
		try
		{
			aClass = (Class<T>)callerClass.forName(className);
		}
		catch (ClassNotFoundException ex)
		{
			return null;
		}
		return this.newInstanceOf(aClass, params);
	} // createInstanceOf() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns all interfaces the given object's class implements.
	 * The resulting array contains all directly implemented interfaces as well
	 * as those that are only indirectly implemented by extension.
	 * If no interface is found an empty array will be returned. 
	 */
	public Class[] getInterfacesOf( Object object ) 
	{
		if ( object == null )
		{
			return EMPTY_CLASS_ARRAY;
		}
		return this.getInterfacesOf( object.getClass() ) ;
	} // getInterfacesOf() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns all interfaces the given class implements.
	 * The resulting array contains all directly implemented interfaces as well
	 * as those that are only indirectly implemented by extension. 
	 * If no interface is found an empty array will be returned. 
	 */
	public Class[] getInterfacesOf( Class aClass ) 
	{
		Set result ;
		
		result = new HashSet(20) ;
		if ( aClass != null )
		{
			this.collectInterfaces( result, aClass ) ;
		}
		if ( result.isEmpty() )
		{
			return EMPTY_CLASS_ARRAY;
		}
		return (Class[]) result.toArray( new Class[result.size()] ) ;
	} // getInterfacesOf() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the method with the specified name if it exists in the given
	 * class. The method will only be found if it has all modifiers set that
	 * are defined in parameter modifiers.
	 * 
	 * @param aClass The class in which to search the method 
	 * @param methodName The name of the searched method
	 * @param paramTypes The types of the method's parameters (null and Class[0] ar the same)
	 * @param modifiers The modifiers that must be set at the method too look for
	 * 
	 * @return The method or null if not found
	 * @see #getMethod(Object, String, Class[])
	 * @see #findMethod(Class, String, Class[])
	 */
	public Method findMethod( Class aClass, String methodName, Class[] paramTypes, int modifiers)
	{
		Method method = null;
		Method[] methods = null;
		Class[] types = null;
		Class[] lookupParamTypes = null;
		Class superclass = null;
		int index = 0;

		if ( paramTypes == null )
		{
			lookupParamTypes = EMPTY_CLASS_ARRAY ;
		}
		else
		{
			lookupParamTypes = paramTypes ;
		}
		
		methods = aClass.getDeclaredMethods();
		for (index = 0; index < methods.length; index++)
		{
			if (methods[index].getName().equals(methodName))
			{
				types = methods[index].getParameterTypes();
				if ( this.compatibleTypes(lookupParamTypes, types) )
				{
					// All specified modifiers must be set
					if ( ( methods[index].getModifiers() & modifiers ) == modifiers )
					{
						return methods[index];						
					}
				}
			}
		} // for
		superclass = aClass.getSuperclass();
		if (superclass != null)
		{
			method = this.findMethod(superclass, methodName, lookupParamTypes, modifiers);
		}
		return method;
	} // findMethod() 
 
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the method with the specified name if it exists in the given
	 * class or any of its superclasses regardless of the method's visibility.
	 * 
	 * @param aClass The class in which to search the method 
	 * @param methodName The name of the searched method
	 * @param paramTypes The types of the method's parameters (null and Class[0] ar the same)
	 * @return The method or null if not found
	 * @see #getMethod(Object, String, Class[])
	 */
	public Method findMethod( Class aClass, String methodName, Class[] paramTypes)
	{
		return this.findMethod( aClass, methodName, paramTypes, 0 ) ;
	} // findMethod() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the method with the specified name if it exists in the class
	 * of the given object or any of its superclasses regardless the 
	 * method's visibility.
	 * 
	 * @param object The object in which the method should be found
	 * @param methodName The name of the searched method
	 * @param paramTypes The types of the method's parameters (null and Class[0] are the same)
	 * @return The method or null if not found
	 * @see #findMethod(Class, String, Class[])
	 */
	public Method getMethod(Object object, String methodName, Class[] paramTypes)
	{
		Class clazz;
		
		if (object == null)
		{
			return null;
		}
		if (object instanceof Class)
		{
			clazz = (Class)object;
		}
		else
		{
			clazz = object.getClass();
		}
		return this.findMethod(clazz, methodName, paramTypes);
	} // getMethod() 
 	
	// -------------------------------------------------------------------------

	/**
	 * Returns a list of all methods the given objects contains.
	 * This includes all inherited methods, regardless of their visibility or
	 * other modifiers.
	 * 
	 * @param obj The object of which to get the methods 
	 * @return A List of java.lang.reflect.Method
	 */
	public List getMethodsOf(Object obj)
	{
		Class aClass;

		aClass = obj.getClass();
		return this.getMethodsOf(aClass);
	} // getMethodsOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Returns a list of all methods the given class contains.
	 * This includes all inherited methods, regardless of their visibility or
	 * other modifiers.
	 * 
	 * @param aClass The class of which to get the methods 
	 * @return A List of java.lang.reflect.Method
	 */
	public List getMethodsOf( Class aClass )
	{
		List methods			= new ArrayList( 40 ) ;
		
		this.addInheritedMethods( methods, aClass.getSuperclass() ) ;
		this.addMethodsToList( methods, aClass.getDeclaredMethods() ) ;
		return methods ;
	} // getMethodsOf() 
	
	// --------------------------------------------------------------------------
	
	/**
	 * Returns a list of all constructors the class of the given object contains.
	 * This includes all constructors, regardless of their visibility or
	 * other modifiers. Even if it has only the default constructor it will
	 * be returned.
	 * 
	 * @param object The object of which to get the constructors 
	 * @return A List of java.lang.reflect.Constructor
	 */
	public List getConstructorsOf( Object object )
	{
		return this.getConstructorsOf( object.getClass() ) ;
	} // getConstructorsOf() 
	
	// --------------------------------------------------------------------------
	
	/**
	 * Returns a list of all constructors the given class contains.
	 * This includes all constructors, regardless of their visibility or
	 * other modifiers. Even if it has only the default constructor it will
	 * be returned.
	 * 
	 * @param aClass The class of which to get the constructors 
	 * @return A List of java.lang.reflect.Constructor
	 */
	public List getConstructorsOf( Class aClass )
	{
		List constructors	= new ArrayList( 10 ) ;
		Constructor[] constructorArray ;
		
		constructorArray = aClass.getDeclaredConstructors() ;
		for ( int i = 0 ; i < constructorArray.length ; i++ )
		{
			constructors.add( constructorArray[i] ) ;
		}
		return constructors ;
	} // getConstructorsOf() 
	
	// --------------------------------------------------------------------------
	
	/**
	 * Returns a list of all fields the given objects contains.
	 * This includes all inherited fields, regardless their visibility or
	 * other modifier states.
	 * 
	 * @param obj The object to get the fields from
	 * @return A List of java.lang.reflect.Field
	 */
	public List getFieldsOf( Object obj )
	{
		Class aClass			= null ;
		
		aClass = obj.getClass() ;
		return this.getFieldsOf( aClass ) ;
	} // getFieldsOf() 
	
	// --------------------------------------------------------------------------
	
	/**
	 * Returns a list of all fields the given class contains.
	 * This includes all inherited fields, regardless their visibility or
	 * other modifier states.
	 * 
	 * @param aClass The class to get the fields from
	 * @return A List of java.lang.reflect.Field
	 */
	public List getFieldsOf( Class aClass )
	{
		List fields				= new ArrayList( 30 ) ;
		
		this.addInheritedFields( fields, aClass.getSuperclass() ) ;
		this.addFieldsToList( fields, aClass.getDeclaredFields() ) ;
		return fields ;
	} // getFieldsOf() 
	
	// --------------------------------------------------------------------------
	
	/**
	 * Returns the field with the specified name in the given class and all
	 * the specified modifiers set.
	 * If the field can't be found, null is returned.
	 * 
	 * @param aClass The class that might contain the field
	 * @param name The name of the field to look for
	 * @param modifiers The modifiers the field must have set
	 * 
	 * @throws IllegalArgumentException If aClass or name is null
	 */
	public Field findField( Class aClass, String name, int modifiers )
	{
		List fields ;
		Iterator iter ;
		Field field ;
	
		if ( ( aClass == null ) || ( name == null ) )
		{
			throw new IllegalArgumentException( "Given class or field name is null" ) ;
		}
		
		fields = this.getFieldsOf( aClass ) ;
		iter = fields.iterator() ;
		while( iter.hasNext() )
		{
			field = (Field)iter.next() ;
			if ( name.equals( field.getName() ) )
			{
				if ( ( field.getModifiers() & modifiers ) == modifiers )
				{					
					return field ;
				}
			}
		}
		return null ;
	} // findField() 
 
	// -------------------------------------------------------------------------	

	/**
	 * Returns the field with the specified name in the given class.
	 * If the field can't be found, null is returned.
	 * 
	 * @param aClass The class that might contain the field
	 * @param name The name of the field to look for
	 * @throws IllegalArgumentException If aClass or name is null
	 */
	public Field findField( Class aClass, String name )
	{
		return this.findField( aClass, name, 0 ) ;
	} // findField() 
	
	// -------------------------------------------------------------------------	
	
	/**
	 * Returns the field with the specified name in the given object.
	 * If the field can't be found, null is returned.
	 * <p>
	 * If the given object is an instance of Class, the field will be looked-up
	 * in the type represented by the class and not in the class object itself.
	 * 
	 * @param object The object that (perhaps) contains the field. This can also be an instance of Class.
	 * @param name The name of the field to look for
	 * @throws IllegalArgumentException If object or name is null
	 */
	public Field getField(Object object, String name)
	{
		Class clazz;

		if ((object == null) || (name == null))
		{
			throw new IllegalArgumentException("Given object or field name is null");
		}
		if (object instanceof Class)
		{
			clazz = (Class)object;
		}
		else
		{
			clazz = object.getClass();
		}
		return this.findField(clazz, name);
	} // getField() 
	
	// -------------------------------------------------------------------------	
	
	/**
	 * Returns the current value of the field with the specified name in the 
	 * given object.
	 * 
	 * @param obj The object that contains the field
	 * @param name The name of the field to look for
	 * @throws NoSuchFieldException If the field is unknown in the given object
	 * @throws IllegalArgumentException If obj or name is null
	 */
	public Object getValueOf( Object obj, String name )
		throws NoSuchFieldException
	{
		Field field ;
		Object value 		= null ;
		boolean saveAccessibility	= false ;
		
		field = this.getField( obj, name ) ;
		if ( field == null )
			throw new NoSuchFieldException( "Field name: " + name ) ;
			
		saveAccessibility = field.isAccessible() ;
		field.setAccessible( true ) ;
		try
		{
			value = field.get(obj) ;
		}
		catch ( NullPointerException ex )
		{
			// Ignore this, because null values are allowed !
		}
		catch ( IllegalAccessException ex1 )
		{
			throw new IllegalAccessError( ex1.getMessage() ) ;
		}
		finally
		{
			field.setAccessible( saveAccessibility ) ;
		}
		
		return value ;
	} // getValueOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Sets the value of the field with the specified name to the 
	 * given value.
	 * 
	 * @param obj The object that contains the field
	 * @param name The name of the field to set
	 * @param value The value to assign to the field
	 * @throws NoSuchFieldException If the field is unknown in the given object
	 * @throws IllegalArgumentException If obj or name is null
	 */
	public void setValueOf( Object obj, String name, Object value )
		throws NoSuchFieldException
	{
		this.setValueOf( obj, name, value, false) ;
	} // setValueOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Sets the value of the field with the specified name to the 
	 * given value.
	 * 
	 * @param obj The object that contains the field
	 * @param name The name of the field to set
	 * @param value The value to assign to the field
	 * @throws NoSuchFieldException If the field is unknown in the given object
	 * @throws IllegalArgumentException If obj or name is null
	 */
	public void setValueOf( Object obj, String name, char value )
		throws NoSuchFieldException
	{
		this.setValueOf( obj, name, Character.valueOf(value), true) ;
	} // setValueOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Sets the value of the field with the specified name to the 
	 * given value.
	 * 
	 * @param obj The object that contains the field
	 * @param name The name of the field to set
	 * @param value The value to assign to the field
	 * @throws NoSuchFieldException If the field is unknown in the given object
	 * @throws IllegalArgumentException If obj or name is null
	 */
	public void setValueOf( Object obj, String name, int value )
		throws NoSuchFieldException
	{
		this.setValueOf( obj, name, Integer.valueOf(value), true) ;
	} // setValueOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Sets the value of the field with the specified name to the 
	 * given value.
	 * 
	 * @param obj The object that contains the field
	 * @param name The name of the field to set
	 * @param value The value to assign to the field
	 * @throws NoSuchFieldException If the field is unknown in the given object
	 * @throws IllegalArgumentException If obj or name is null
	 */
	public void setValueOf( Object obj, String name, byte value )
		throws NoSuchFieldException
	{
		this.setValueOf( obj, name, Byte.valueOf(value), true) ;
	} // setValueOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Sets the value of the field with the specified name to the 
	 * given value.
	 * 
	 * @param obj The object that contains the field
	 * @param name The name of the field to set
	 * @param value The value to assign to the field
	 * @throws NoSuchFieldException If the field is unknown in the given object
	 * @throws IllegalArgumentException If obj or name is null
	 */
	public void setValueOf( Object obj, String name, boolean value )
		throws NoSuchFieldException
	{
		this.setValueOf( obj, name, value ? Boolean.TRUE : Boolean.FALSE , true) ;
	} // setValueOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Sets the value of the field with the specified name to the 
	 * given value.
	 * 
	 * @param obj The object that contains the field
	 * @param name The name of the field to set
	 * @param value The value to assign to the field
	 * @throws NoSuchFieldException If the field is unknown in the given object
	 * @throws IllegalArgumentException If obj or name is null
	 */
	public void setValueOf( Object obj, String name, long value )
		throws NoSuchFieldException
	{
		this.setValueOf( obj, name, Long.valueOf(value), true) ;
	} // setValueOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Sets the value of the field with the specified name to the 
	 * given value.
	 * 
	 * @param obj The object that contains the field
	 * @param name The name of the field to set
	 * @param value The value to assign to the field
	 * @throws NoSuchFieldException If the field is unknown in the given object
	 * @throws IllegalArgumentException If obj or name is null
	 */
	public void setValueOf( Object obj, String name, short value )
		throws NoSuchFieldException
	{
		this.setValueOf( obj, name, Short.valueOf(value), true) ;
	} // setValueOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Sets the value of the field with the specified name to the 
	 * given value.
	 * 
	 * @param obj The object that contains the field
	 * @param name The name of the field to set
	 * @param value The value to assign to the field
	 * @throws NoSuchFieldException If the field is unknown in the given object
	 * @throws IllegalArgumentException If obj or name is null
	 */
	public void setValueOf( Object obj, String name, double value )
		throws NoSuchFieldException
	{
		this.setValueOf( obj, name, Double.valueOf(value), true) ;
	} // setValueOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Sets the value of the field with the specified name to the 
	 * given value.
	 * 
	 * @param obj The object that contains the field
	 * @param name The name of the field to set
	 * @param value The value to assign to the field
	 * @throws NoSuchFieldException If the field is unknown in the given object
	 * @throws IllegalArgumentException If obj or name is null
	 */
	public void setValueOf( Object obj, String name, float value )
		throws NoSuchFieldException
	{
		this.setValueOf( obj, name, Float.valueOf(value), true) ;
	} // setValueOf() 
 
	// --------------------------------------------------------------------------

	/**
	 * Returns true if a public method with the specified name exists in
	 * the given class or any of its superclasses.
	 * 
	 * @param aClass The class in which to look for the method 
	 * @param methodName The name of the method to look for
	 * @param paramTypes The types of the method's parameters (null and Class[0] ar the same)
	 * 
	 * @return true if the method was found and is public
	 */
	public boolean hasPublicMethod( Class aClass, String methodName, Class[] paramTypes)
	{
		Method method ;
		
		if ( aClass == null )
		{
			return false ;
		}
		method = this.findMethod( aClass, methodName, paramTypes, Modifier.PUBLIC ) ;
		return ( method != null ) ;
	} // hasPublicMethod() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if a public method with the specified name exists in the class
	 * of the given object or any of its superclasses.
	 * 
	 * @param obj The object in which the method should be found
	 * @param methodName The name of the method to look for
	 * @param paramTypes The types of the method's parameters (null and Class[0] ar the same)
	 * 
	 * @return true if the method was found and is public
	 */
	public boolean hasPublicMethod( Object obj, String methodName, Class[] paramTypes)
	{
		if ( obj == null )
		{
			return false ;
		}
		return this.hasPublicMethod( obj.getClass(), methodName, paramTypes ) ;
	} // hasPublicMethod() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the given field is not null and is package visible.
	 */
	public boolean isPackageVisible( Field field ) 
	{
		if ( field == null )
		{
			return false ;
		}
		return this.isDefaultVisibility( field.getModifiers() ) ;
	} // isPackageVisible() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the given method is not null and is package visible.
	 */
	public boolean isPackageVisible( Method method ) 
	{
		if ( method == null )
		{
			return false ;
		}
		return this.isDefaultVisibility( method.getModifiers() ) ;
	} // isPackageVisible() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true, if the visibility defined by the given modifiers
	 * is the default (package) visibility.
	 */
	public boolean isDefaultVisibility( int modifiers )
	{
		final int explicitVisibility = 	Modifier.PUBLIC | 
																		Modifier.PROTECTED | Modifier.PRIVATE ;
		
		return ( ( modifiers & explicitVisibility ) == 0 ) ;
	} // isDefaultVisibility() 
 
  // -------------------------------------------------------------------------

	/**
	 * Returns the visibility defined by the given modifiers as string.
	 * That is, "" for the default (package) visibility and "public",
	 * "protected", "private" for the others.
	 */
	public String getVisibility( int modifiers )
	{
		if ( Modifier.isPublic( modifiers ) )
		{
			return "public" ;
		} 	  
		if ( Modifier.isProtected( modifiers ) )
		{
			return "protected" ;
		} 	  
		if ( Modifier.isPrivate( modifiers ) )
		{
			return "private" ;
		} 	 
		return "" ;
	} // getVisibility() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * If the given class has a constructor without parameters it will be used
	 * to create a new instance. The constructor will be called regardless of
	 * its visibility. That is, even private, protected and default constructors
	 * are used to create the new instance.
	 * 
	 * @param aClass The class of which a new instance must be created (must not be null)
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public <T> T newInstance(Class<T> aClass)
	{
		return this.newInstance(aClass, null);
	} // newInstance() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * If the given class has a constructor with one parameter type matching the
	 * given parameter it will be used to create a new instance. 
	 * The constructor will be called regardless of
	 * its visibility. That is, even private, protected and default constructors
	 * are used to create the new instance.
	 * 
	 * @param aClass The class of which a new instance must be created (must not be null)
	 * @param param The initialization parameter for the constructor (must not be null)
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public <T> T newInstance(Class<T> aClass, Object param)
	{
		return this.newInstance(aClass, new Object[] { param });
	} // newInstance() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * If the given class has a constructor with two parameter type matching the
	 * given parameters it will be used to create a new instance. 
	 * The constructor will be called regardless of
	 * its visibility. That is, even private, protected and default constructors
	 * are used to create the new instance.
	 * 
	 * @param aClass The class of which a new instance must be created (must not be null)
	 * @param param1 The first initialization parameter for the constructor (must not be null)
	 * @param param2 The second initialization parameter for the constructor (must not be null)
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public <T> T newInstance(Class<T> aClass, Object param1, Object param2)
	{
		return this.newInstance(aClass, new Object[] { param1, param2 });
	} // newInstance() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * If the given class has a constructor with types corresponding to the given
	 * parameters it will be used to create a new instance. 
	 * The constructor will be called regardless of
	 * its visibility. That is, even private, protected and default constructors
	 * are used to create the new instance.
	 * <br>
	 * This method is exactly the same as {@link #newInstanceOf(Class, Object...)}.
	 * It is just kept for compatibility.
	 * 
	 * @param aClass The class of which a new instance must be created (must not be null)
	 * @param params The initialization parameters for the constructor (may be null)
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public <T> T newInstance(Class<T> aClass, Object[] params)
	{
		return this.newInstanceOf(aClass, params) ;
	} // newInstance() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * If the given class has a constructor with types corresponding to the given
	 * parameters it will be used to create a new instance. 
	 * The constructor will be called regardless of
	 * its visibility. That is, even private, protected and default constructors
	 * are used to create the new instance.
	 * <br>
	 * This method is exactly the same as {@link #newInstance(Class, Object[])}.
	 * It is just uses the varargs declaration that is available since Java 1.5.
	 * 
	 * @param aClass The class of which a new instance must be created (must not be null)
	 * @param params The initialization parameters for the constructor (may be null)
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public <T> T newInstanceOf(Class<T> aClass, Object... params)
	{
		Constructor<?> constructor ;
		boolean accessible ;
		Exception ex = null ;
		Class[] paramTypes ;
		
		paramTypes = this.getTypesFromParameters( params ) ;
		constructor = this.findConstructor( aClass, paramTypes ) ;
		if ( constructor != null )
		{
			accessible = constructor.isAccessible() ;
			constructor.setAccessible( true ) ;
			try
			{
				return (T) constructor.newInstance(params) ;
			}
			catch ( Exception e )
			{
				ex = e ;
			}
			finally
			{
				constructor.setAccessible( accessible ) ;
			}
			if ( ex != null )
			{
				throw new ReflectionException( ex ) ;
			}
		}
		return null ;
	} // newInstanceOf() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Tries to find the class with the given name and to create an instance of it.
	 * If the given class has a constructor without parameters it will be used
	 * to create a new instance. The constructor will be called regardless of
	 * its visibility. That is, even private, protected and default constructors
	 * are used to create the new instance.
	 * 
	 * @param className The name of the class of which a new instance must be created (must not be null)
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public Object newInstance(String className)
	{
		return this.newInstance(className, null);
	} // newInstance() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Tries to find the class with the given name and to create an instance of it.
	 * If the given class has a constructor with one parameter type matching the
	 * given parameter it will be used to create a new instance. 
	 * The constructor will be called regardless of
	 * its visibility. That is, even private, protected and default constructors
	 * are used to create the new instance.
	 * 
	 * @param className The name of the class of which a new instance must be created (must not be null)
	 * @param param The initialization parameter for the constructor (must not be null)
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public Object newInstance(String className, Object param)
	{
		return this.newInstance(className, new Object[] { param });
	} // newInstance() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Tries to find the class with the given name and to create an instance of it.
	 * If the given class has a constructor with two parameter type matching the
	 * given parameters it will be used to create a new instance. 
	 * The constructor will be called regardless of
	 * its visibility. That is, even private, protected and default constructors
	 * are used to create the new instance.
	 * 
	 * @param className The name of the class of which a new instance must be created (must not be null)
	 * @param param1 The first initialization parameter for the constructor (must not be null)
	 * @param param2 The second initialization parameter for the constructor (must not be null)
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public Object newInstance( String className, Object param1, Object param2 )
	{
		return this.newInstance( className, new Object[] { param1, param2 } ) ;
	} // newInstance() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Tries to find the class with the given name and to create an instance of it.
	 * If the found class has a constructor with types corresponding to the given
	 * parameters it will be used to create a new instance. 
	 * The constructor will be called regardless of
	 * its visibility. That is, even private, protected and default constructors
	 * are used to create the new instance.
	 * 
	 * @param className The name of the class of which a new instance must be created (must not be null)
	 * @param params The initialization parameters for the constructor (may be null)
	 * @return The new created instance or null if no matching constructor can be found 
	 * @throws ReflectionException A runtime exception that wraps the original exception.
	 */
	public Object newInstance( String className, Object[] params )
	{
		Class clazz = null ;
		
		try
		{
			clazz = this.getLoader().loadClass( className ) ;
		}
		catch ( ClassNotFoundException e )
		{
			throw new ReflectionException(e) ;
		}
		return this.newInstance( clazz, params ) ;
	} // newInstance() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the constructor of the given class for the specified parameter
	 * types of null if no such constructor can be found.
	 * The visibility of the constructor is ignored. A private constructor
	 * can be used with the newInstance() methods of this class to create
	 * instances. 
	 * 
	 * @return A constructor or null
	 * @see #newInstance(Class)
	 */
	public <T> Constructor<?> findConstructor( Class<T> aClass, Class[] paramTypes )
	{
		Constructor<?>[] constructors;
		Class[] types;

		if ( paramTypes == null )
		{
			paramTypes = new Class[0];
		}

		constructors = aClass.getDeclaredConstructors();
		for (int i = 0; i < constructors.length; i++ )
		{
			types = constructors[i].getParameterTypes();
			if ( this.compatibleTypes( paramTypes, types ) )
			{
				return constructors[i];
			}
		} 
		return null ;
	} // findConstructor() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns an array of the types (classes) corresponding to the parameter
	 * objects given to this methods.
	 * 
	 * @param params The parameters to derive the types from (may be null)
	 * @return The types or an empty array if params == null
	 */
	public Class[] getTypesFromParameters( Object[] params ) 
	{
		Class[] types ;
		
		if ( params == null )
		{
			return EMPTY_CLASS_ARRAY ;
		}
		types = new Class[params.length] ;
		for (int i = 0; i < params.length; i++ )
		{
			types[i] = this.getTypeOf( params[i] ) ;
		}
		return types ;
	} // getTypesFromParameters() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the type of the given object. For the special objects like
	 * Integer, Boolean, ... it returns the primitive type.
	 * If the given object is null it returns Object.class
	 * 
	 * @param object The object of which to determine the type 
	 */
	public Class getTypeOf(Object object)
	{
		if ( object == null )
		{
			return Object.class ;
		}
		if (object instanceof Integer)
			return Integer.TYPE;
		if (object instanceof Boolean)
			return Boolean.TYPE;
		if (object instanceof Long)
			return Long.TYPE;
		if (object instanceof Short)
			return Short.TYPE;
		if (object instanceof Double)
			return Double.TYPE;
		if (object instanceof Float)
			return Float.TYPE;
		if (object instanceof Character)
			return Character.TYPE;
		if (object instanceof Byte)
			return Byte.TYPE;

		return object.getClass();
	} // getTypeOf() 

	// -------------------------------------------------------------------------

  /**
   * Returns all types of the given object or an empty collection if the object is null.
   * The returned array contains the object's class and all interfaces it implements,
   * including all inherited interfaces.
   * 
   * @param object The object to derive all types of
   */
  public List<Class> getAllTypesOf(Object object) 
  {
    Class baseType;
    List<Class> types;
    
    types = new ArrayList<Class>(); 
    if (object == null)
    {
      return types;
    }
    baseType = this.getTypeOf(object);
    types.add(baseType);
    if (!baseType.isPrimitive())
    {      
    	types.addAll(Arrays.asList(this.getInterfacesOf(object.getClass())));
    }
    return types;
  } // getAllTypesOf()
  
  // -------------------------------------------------------------------------
  
	/**
	 * Returns true if the given class is found in the provided class array.
	 */
	public boolean contains( Class[] classes, Class aClass ) 
	{
		return this.indexOf( classes, aClass ) >= 0 ;
	} // contains() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the index of the given class in the provided class array or -1
	 * if the class is not in the array.
	 */
	public int indexOf( Class[] classes, Class aClass ) 
	{
		if ( this.isNullOrEmpty( classes ) || ( aClass == null ) )
		{
			return NOT_FOUND;
		}
		for (int i = 0; i < classes.length; i++ )
		{
			if ( aClass.equals( classes[i] ) )
			{
				return i;
			}
		}
		return NOT_FOUND;
	} // indexOf() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the class of the given object implements the 
	 * specified interfaceType.
	 */
	public boolean implementsInterface( Object object, Class anInterface ) 
	{
		if ( ( object == null ) || ( anInterface == null ) )
		{
			return false ;
		}
		return this.implementsInterface( object.getClass(), anInterface ) ;
	} // implementsInterface() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the given class implements the specified interfaceType.
	 */
	public boolean implementsInterface( Class aClass, Class anInterface ) 
	{
		Class[] interfaces ;
		
		if ( ( aClass == null ) || ( anInterface == null ) )
		{
			return false ;
		}
		if ( aClass.isInterface() || !anInterface.isInterface() )
		{
			return false ;
		}
		interfaces = this.getInterfacesOf( aClass ) ;
		return this.contains( interfaces, anInterface ) ;
	} // implementsInterface() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns an array containing all objects that are returned by the specified
	 * method name executed against each element in the given collection.
	 * 
	 * @param coll The collection with elements of which the objects have to be extracted
	 * @param methodName The name of the method to be executed on the collections's elements (must not be null)
	 * This method must have no argument and must return an object of the specified elementType.
	 * @param elementType The type of the elements returned by the method and of the elements in the return array
	 * @return Returns an array of the specified elementType or null if the given collection is null. 
	 */
	public <T> T[] toArray(Collection coll, String methodName, Class<T> elementType) 
	{
		T[] objects ;
		Object element ;
		Iterator iter ;
		int i = 0;
		
		if ( coll == null )
		{
			return null;
		}
  	
		objects = (T[])Array.newInstance(elementType, coll.size() ) ;
		iter = coll.iterator() ;
		while (iter.hasNext())
		{
			element = iter.next();
			if ("this".equals(methodName) && (elementType.isInstance(element)))
			{
				objects[i] = (T)element;
			}
			else
			{
				objects[i] = (T)Dynamic.invoke(element, methodName);
			}
			i++;
		}
  	return objects;
	} // toArray() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns a string array containing all objects that are returned by the specified
	 * method name executed against each element in the given collection.
	 * 
	 * @param coll The collection with elements of which the strings have to be extracted
	 * @param methodName The name of the method to be executed on the collections's elements (must not be null).
	 * This method must have no argument and must return a string.
	 * @return Returns a string array or null if the given collection is null. 
	 */
	public String[] toStringArray(Collection coll, String methodName)
	{
		return this.toArray(coll, methodName, String.class);
	} // toStringArray() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Tries to find the annotation of the specified type and return its "value"
	 * as String.
	 * 
	 * @param aClass The potentionally annotated class
	 * @param annotationType The annotation to look for
	 * @return The value string of the annotation of null if not found
	 */
  public String getAnnotationValueFrom(Class aClass, Class<? extends Annotation> annotationType) 
	{
		Annotation annotation;
		
		annotation = aClass.getAnnotation(annotationType);
		if (annotation == null)
		{
			return null;
		}
		return (String)Dynamic.invoke(annotation, "value");
	} // getAnnotationValueFrom()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void addMethodsToList( List methodList, Method[] methods )
	{
		for ( int i = 0 ; i < methods.length ; i++ )
		{
			methodList.add( methods[i] ) ;
		}
	} // addMethodsToList() 
 
	// --------------------------------------------------------------------------

	protected void addInheritedMethods( List methods, Class aClass )
		throws SecurityException 
	{
		if ( aClass != null )
		{
			this.addInheritedMethods( methods, aClass.getSuperclass() ) ;
			this.addMethodsToList( methods, aClass.getDeclaredMethods() ) ;
		}
	} // addInheritedMethods() 
 
	// --------------------------------------------------------------------------
 
	protected void addFieldsToList( List fieldList, Field[] fields )
	{
		for ( int i = 0 ; i < fields.length ; i++ )
		{
			fieldList.add( fields[i] ) ;
		}
	} // addFieldsToList() 
	
	// --------------------------------------------------------------------------
	
	protected void addInheritedFields( List fields, Class aClass )
	throws SecurityException 
	{
		if ( aClass != null )
		{
			this.addInheritedFields( fields, aClass.getSuperclass() ) ;
			this.addFieldsToList( fields, aClass.getDeclaredFields() ) ;
		}
	} // addInheritedFields() 
	
	// --------------------------------------------------------------------------
	
	protected void setValueOf(Object obj, String name, Object value, boolean isPrimitive) throws NoSuchFieldException
	{
		Field field;
		boolean saveAccessibility = false;

		field = this.getField(obj, name);
		if (field == null)
			throw new NoSuchFieldException("Field name: " + name);

		saveAccessibility = field.isAccessible();
		field.setAccessible(true);
		try
		{
			if (isPrimitive)
			{
				if (value instanceof Character)
				{
					field.setChar(obj, ((Character)value).charValue());
				}
				else if (value instanceof Integer)
				{
					field.setInt(obj, ((Integer)value).intValue());
				}
				else if (value instanceof Long)
				{
					field.setLong(obj, ((Long)value).longValue());
				}
				else if (value instanceof Boolean)
				{
					field.setBoolean(obj, ((Boolean)value).booleanValue());
				}
				else if (value instanceof Double)
				{
					field.setDouble(obj, ((Double)value).doubleValue());
				}
				else if (value instanceof Float)
				{
					field.setFloat(obj, ((Float)value).floatValue());
				}
				else if (value instanceof Byte)
				{
					field.setByte(obj, ((Byte)value).byteValue());
				}
				else if (value instanceof Short)
				{
					field.setShort(obj, ((Short)value).shortValue());
				}
			}
			else
			{
				field.set(obj, value);
			}
		}
		catch (NullPointerException ex)
		{
			// Ignore this, because null values are allowed !
		}
		catch (IllegalAccessException ex1)
		{
			throw new IllegalAccessError(ex1.getMessage());
		}
		finally
		{
			field.setAccessible(saveAccessibility);
		}
	} // setValueOf() 
 
	// -------------------------------------------------------------------------
	
	/**
	 * Returns true if the types of the first array are assignable to the 
	 * types of the second array. The second array is immutable.
	 */
	protected boolean compatibleTypes( Class[] paramTypes, Class[] signatureTypes ) 
	{
    if ( paramTypes == null )
    {
      return ( signatureTypes == null ) ;
    }
    if ( signatureTypes == null )
    {
      return false ;
    }
		if ( paramTypes.length != signatureTypes.length )
		{
			return false ;
		}
		
		for (int i = 0; i < paramTypes.length; i++ )
		{
			if ( ! signatureTypes[i].isAssignableFrom( paramTypes[i] ) )
			{
				return false ;
			}
		}
		return true ;
	} // compatibleTypes() 

	// -------------------------------------------------------------------------

	protected void collectInterfaces( Set result, Class aClass ) 
	{
		Class[] interfaces ;
		
		if ( aClass == null )
		{
			return ;
		}
		if ( aClass.isInterface() )
		{
			result.add( aClass ) ;
		}
		else
		{
			this.collectInterfaces( result, aClass.getSuperclass() ) ;			
		}
		interfaces = aClass.getInterfaces() ;
		for (int i = 0; i < interfaces.length; i++ )
		{
			this.collectInterfaces( result, interfaces[i] ) ;
		}
	} // collectInterfaces() 
	
	// -------------------------------------------------------------------------

	protected boolean isNullOrEmpty( Object[] objects ) 
	{
		return ( objects == null ) || ( objects.length == 0 ) ;
	} // isNullOrEmpty() 
	
	// -------------------------------------------------------------------------
	
	protected boolean isNullOrEmpty( Collection collection ) 
	{
		return ( collection == null ) || ( collection.isEmpty() ) ;
	} // isNullOrEmpty() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the externally assigned class loader or if not present the class
	 * load of this class.
	 */
	protected ClassLoader getLoader() 
	{
		if ( loader != null )
		{
			return loader ;
		}
		return this.getClass().getClassLoader() ;
	} // getLoader() 
	
	// -------------------------------------------------------------------------
	
} // class ReflectUtil 
