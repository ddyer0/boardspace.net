// ===========================================================================
// CONTENT  : CLASS Dynamic
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.5 - 13/11/2009
// HISTORY  :
//  21/12/1999  duma  CREATED
//	24/01/2000	duma	MOVED		-->	from package 'com.mdcs.util' to 'org.pf.util'
//	28/09/2002	duma	MOVED		-->	from package 'org.pf.util' to 'org.pf.reflect'
//	14/12/2002	duma	added		-->	invoke() and perform() with argTypes
//	06/03/2004	duma	fixed		--> don't use new Boolean(boolean)
//	25/02/2007	mdu		added		--> perform(Object receiver, Method method, Object[] args )
//	13/11/2009	mdu		changed	--> Object perform(Object receiver, String methodName, Object[] args, Class[] argTypes)
//
// Copyright (c) 1999-2009, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.reflect;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Method;

/**
 * This class supports more dynamic programming than usually possible in Java.
 * With the class method <i>perform</i> in different variants it provides a
 * more convenient dynamic method invocation service, than the pure reflection api.
 * Especially Smalltalk programmers will find some similarities to their language. <br>
 *
 * To be able using this class, when a security manager is installed, you'll
 * have to grant <br>
 * <b>permission java.lang.reflect.ReflectPermission "supressAccessChecks" ; </b><br>
 * in the policy file. Otherwise any dynamic method invocation will cause a
 * AccessControlException.
 *
 * @author Manfred Duchrow
 * @version 1.5
 * @see java.lang.reflect.ReflectPermission
 */
abstract public class Dynamic
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================

	// =========================================================================
	// CLASS METHODS
	// =========================================================================

	/**
	 * Executes the named method on the given object and without any arguments.
	 * If an exception occurs it will be returned as the result. 
	 * Otherwise the result of the invoked method will be returned.
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 */
	public static Object invoke(Object receiver, String methodName)
	{
		Object result = null;
		try
		{
			result = perform(receiver, methodName);
		}
		catch (Throwable ex)
		{
			result = ex;
		}
		return result;
	} // invoke() 

	// -------------------------------------------------------------------------

	/**
	 * Executes the named method on the given object with one none primitive argument.
	 * <p>
	 * If an exception occurs during this method invocation the result will be
	 * the exception rather than the expected value of the method call!
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @param arg An object, which becomes the only argument for the called method
	 */
	public static Object invoke(Object receiver, String methodName, Object arg)
	{
		Object result = null;
		try
		{
			result = perform(receiver, methodName, arg);
		}
		catch (Throwable ex)
		{
			result = ex;
		}
		return result;
	} // invoke() 

	// -------------------------------------------------------------------------

	/**
	 * Executes the named method on the given object with one integer argument.
	 * <p>
	 * If an exception occurs during this method invocation the result will be
	 * the exception rather than the expected value of the method call!
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @param arg An integer value, which becomes the only argument for the called method
	 */
	public static Object invoke(Object receiver, String methodName, int arg)
	{
		Object result = null;
		try
		{
			result = perform(receiver, methodName, arg);
		}
		catch (Throwable ex)
		{
			result = ex;
		}
		return result;
	} // invoke() 

	// -------------------------------------------------------------------------

	/**
	 * Executes the named method on the given object with one boolean argument.
	 * <p>
	 * If an exception occurs during this method invocation the result will be
	 * the exception rather than the expected value of the method call!
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @param arg A boolean value, which becomes the only argument for the called method
	 */
	public static Object invoke(Object receiver, String methodName, boolean arg)
	{
		Object result = null;
		try
		{
			result = perform(receiver, methodName, arg);
		}
		catch (Throwable ex)
		{
			result = ex;
		}
		return result;
	} // invoke() 

	// -------------------------------------------------------------------------

	/**
	 * Executes the named method on the given object with several arguments.   <br>
	 * Arguments that are instances of classes that correspond to primitive types
	 * will be automatically converted into the primitive types.<br>
	 * Integer -> int   <br>
	 * Boolean -> boolean   <br>
	 * Long -> long   <br>
	 * Double -> double   <br>
	 * Float -> float   <br>
	 * Byte - byte   <br>
	 * Character -> char   <br>
	 * Short -> short   <br>
	 * <p>
	 * If an exception occurs during this method invocation the result will be
	 * the exception rather than the expected value of the method call!
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @param args The arguments for the method invocation
	 */
	public static Object invoke(Object receiver, String methodName, Object[] args)
	{
		Object result = null;
		try
		{
			result = perform(receiver, methodName, args);
		}
		catch (Throwable ex)
		{
			result = ex;
		}
		return result;
	} // invoke() 

	// -------------------------------------------------------------------------

	/**
	 * Executes the named method on the given object with several arguments.   <br>
	 * Here the types of the arguments the method expects can be defined
	 * explicitly to detect the correct method signature.
	 * <p>
	 * If an exception occurs during this method invocation the result will be
	 * the exception rather than the expected value of the method call!
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @param args The arguments for the method invocation
	 * @param argTypes The types of the arguments as defined in the method signature
	 */
	public static Object invoke(Object receiver, String methodName, Object[] args,
															Class[] argTypes )
	{
		Object result = null;
		try
		{
			result = perform(receiver, methodName, args, argTypes );
		}
		catch (Throwable ex)
		{
			result = ex;
		}
		return result;
	} // invoke() 

	// -------------------------------------------------------------------------

	/**
	 * Executes the given method on the given object with the specified arguments.   <br>
	 * <p>
	 * If an exception occurs during this method invocation the result will be
	 * the exception rather than the expected value of the method call!
	 *
	 * @param receiver The object the method should be invoked on.
	 * @param method The method to invoke.
	 * @param args The arguments for the method invocation
	 */
	public static Object invoke(Object receiver, Method method, Object[] args )
	{
		Object result = null;
		try
		{
			result = perform(receiver, method, args );
		}
		catch (Throwable ex)
		{
			result = ex;
		}
		return result;
	} // invoke() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Executes the named method on the given object and without any arguments.
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @exception Exception All kinds of exceptions are passed to the caller
	 */
	public static Object perform(Object receiver, String methodName) throws Exception
	{
		return perform(receiver, methodName, new Object[0], new Class[0]);
	} // perform() 

	// -------------------------------------------------------------------------

	/**
	 * Executes the named method on the given object with one none primitive argument.
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @param arg An object, which becomes the only argument for the called method
	 * @exception Exception All kinds of exceptions are passed to the caller
	 */
	public static Object perform(Object receiver, String methodName, Object arg) throws Exception
	{
		Object[] args = { arg };
		Class[] types = { getTypeOf(arg)};
		return perform(receiver, methodName, args, types);
	} // perform() 

	// -------------------------------------------------------------------------

	/**
	 * Executes the named method on the given object with one integer argument.
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @param arg An integer value, which becomes the only argument for the called method
	 * @exception Exception All kinds of exceptions are passed to the caller
	 */
	public static Object perform(Object receiver, String methodName, int arg) throws Exception
	{
		Object[] args = { Integer.valueOf(arg)};
		Class[] types = { Integer.TYPE };
		return perform(receiver, methodName, args, types);
	} // perform() 

	// -------------------------------------------------------------------------

	/**
	 * Executes the named method on the given object with one boolean argument.
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @param arg A boolean value, which becomes the only argument for the called method
	 * @exception Exception All kinds of exceptions are passed to the caller
	 */
	public static Object perform(Object receiver, String methodName, boolean arg) 
		throws Exception
	{
		Object[] args = { arg ? Boolean.TRUE : Boolean.FALSE } ;
		Class[] types = { Boolean.TYPE };
		return perform(receiver, methodName, args, types);
	} // perform() 

	// -------------------------------------------------------------------------

	/**
	 * Executes the named method on the given object with several arguments.   <br>
	 * Arguments that are instances of classes that correspond to primitive types
	 * will be automatically converted into the primitive types.<br>
	 * Integer -> int   <br>
	 * Boolean -> boolean   <br>
	 * Long -> long   <br>
	 * Double -> double   <br>
	 * Float -> float   <br>
	 * Byte - byte   <br>
	 * Character -> char   <br>
	 * Short -> short   <br>
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @param args The arguments for the method call
	 * @exception Exception All kinds of exceptions are passed to the caller
	 */
	public static Object perform(Object receiver, String methodName, Object[] args) 
		throws Exception
	{
		Class[] types = new Class[args.length];

		for (int i = 0; i < args.length; i++)
		{
			types[i] = getTypeOf(args[i]);
		}
		return perform(receiver, methodName, args, types);
	} // perform() 

	// -------------------------------------------------------------------------

	/**
	 * This method allows direct dynamic execution of JMX MBean method invocations.
	 *  
	 * @param receiver The object on which the method must be invoked
	 * @param methodName The name of the method to invoke
	 * @param args The arguments to be passed to the method
	 * @param signature The qualified class names of the argument types
	 * @return The result of the method execution.
	 */
  public static Object perform(Object receiver, String methodName, Object[] args, String[] signature) throws Exception
  {
    Class[] paramTypes = null;
    if (!isNullOrEmpty(signature))
    {
      paramTypes = new Class[signature.length];
      for (int i = 0; i < paramTypes.length; i++)
      {
        paramTypes[i] = ReflectUtil.current().findClass(signature[i]);
        if (paramTypes[i] == null)
        {
          throw new ClassNotFoundException("Class " + signature[i] + " not found for argument type.");
        }
      }
    }
    return Dynamic.perform(receiver, methodName, args, paramTypes);
  } // perform()
  
  // -------------------------------------------------------------------------

	/**
	 * Executes the named method on the given object with several arguments.   <br>
	 * Here the types of the arguments can be defined explicitly rather than
	 * being determined automatically.
	 *
	 * @param receiver The object the method should be performed on.
	 * @param methodName The name of the method to perform.
	 * @param args The arguments for the method call
	 * @param argTypes The types of the arguments in the args parameter
	 * @exception Exception All kinds of exceptions are passed to the caller
	 */
	public static Object perform(Object receiver, String methodName, Object[] args, 
															Class[] argTypes) 
		throws Exception
	{
		Class aClass ;
		Method method ;

		if (receiver == null)
		{
			throw (new IllegalArgumentException( "The receiver of a method invocation must not be null!" ));
		}

		// Removed the line below because there is no reason why dynamic method invocation
		// on Class object should not be supported.
//		aClass = (receiver instanceof Class) ? (Class) receiver : receiver.getClass();
		aClass = receiver.getClass();
		method = findMethod(aClass, methodName, argTypes);

		if (method == null)
		{
			StringBuffer msg = new StringBuffer(80) ;
			msg.append( "No such method: " ) ;
			msg.append( aClass.getName() ) ;
			msg.append( "." ) ;
			msg.append( methodName ) ;
			msg.append( "(" );
			if ( argTypes != null )
			{
				for (int i = 0; i < argTypes.length; i++ )
				{
					if ( i > 0 )
					{
						msg.append( "," );
					}
					msg.append( argTypes[i].getName() );
				}
			}
			msg.append( ")" );
			throw (new Exception(msg.toString()));
		}
		return perform( receiver, method, args ) ;
	} // perform() 

	// -------------------------------------------------------------------------
	
	/**
	 * Executes the given method on the given object with the specified arguments.   <br>
	 *
	 * @param receiver The object the method should be performed on.
	 * @param method The method to perform.
	 * @param args The arguments for the method call
	 * @exception Exception All kinds of exceptions are passed to the caller
	 */
	public static Object perform(Object receiver, Method method, Object[] args ) throws Exception
	{
		Object result = null;
		boolean saveAccessibility = false;
		
		if (receiver == null)
		{
			throw (new IllegalArgumentException( "The receiver of a method invocation must not be null!" ));
		}
		if (method == null)
		{
			throw (new IllegalArgumentException( "The method to invoke must not be null!" ));
		}
				
		saveAccessibility = true;//method.canAccess(receiver);
		method.setAccessible(true);
		try
		{
			result = method.invoke(receiver, args);
		}
		finally
		{
			method.setAccessible(saveAccessibility);
		}
		
		return result;
	} // perform() 
	
	// -------------------------------------------------------------------------
	
	// =========================================================================
	// PROTECTED CLASS METHODS
	// =========================================================================
	protected static Class getTypeOf(Object object)
	{
		if ( object == null )
		{
			throw new NullPointerException( "Type of null value cannot be determined" ) ;
		}
		return ReflectUtil.current().getTypeOf( object ) ;
	} // getTypeOf() 

	// -------------------------------------------------------------------------

	protected static Method findMethod(Class aClass, String methodName, Class[] paramTypes)
	{
		return ReflectUtil.current().findMethod(aClass, methodName, paramTypes);
	} // findMethod() 

	// -------------------------------------------------------------------------

	private static boolean isNullOrEmpty(String[] strings)
	{
		return (strings == null) || (strings.length == 0);
	} // isNullOrEmpty()
	
	// -------------------------------------------------------------------------
	
} // class Dynamic 
