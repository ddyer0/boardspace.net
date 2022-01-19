// ===========================================================================
// CONTENT  : CLASS ClassInfo
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.4 - 17/11/2010
// HISTORY  :
//  25/01/2003  mdu  CREATED
//	27/02/2007	mdu		adedd		-->	isAssignableFrom(), isAssignableTo(), isInstance()
//	16/11/2007	mdu		added		-->	getInstance()
//	17/11/2010	mdu	changed		-->	to generic type
//
// Copyright (c) 2003-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.reflect ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

/**
 * Contains the name of a class and the class itself. It can create new 
 * instances.
 *
 * @author Manfred Duchrow
 * @version 1.4
 */
public class ClassInfo<T>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0] ; 

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String className = null ;
  protected String className() { return className ; }
  protected void className( String newValue ) { className = newValue ; }
  
	private Class<T> classObject = null ;
  protected Class<T> classObject() { return classObject ; }
  protected void classObject( Class<T> newValue ) { classObject = newValue ; }
    
  private boolean isSingleton = false ;
  public boolean isSingleton() { return isSingleton ; }
  public void setIsSingleton( boolean newValue ) { isSingleton = newValue ; }
  
  private T soleInstance = null ;
  protected T getSoleInstance() { return soleInstance ; }
  protected void setSoleInstance( T newValue ) { soleInstance = newValue ; }
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a class name.
   * 
   * @param className The name of the class the new object should represent.
   */
  public ClassInfo( String className )
  {
    super() ;
    this.setClassName( className ) ;
  } // ClassInfo() 

	// -------------------------------------------------------------------------
	
  /**
   * Initialize the new instance with a class name and a flag if it is a
   * singleton.
   * 
   * @param className The name of the class the new object should represent.
   * @param singleton if true method getInstance() will return always the identical instance
   */
  public ClassInfo( String className, boolean singleton )
  {
  	this( className ) ;
  	this.setIsSingleton( singleton ) ;
  } // ClassInfo() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a class.
   * 
   * @param aClass The class the new object should represent.
   */
	public ClassInfo( Class aClass )
	{
		super() ;
		this.setClassObject( aClass ) ;
	} // ClassInfo() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a class.
	 * 
	 * @param aClass The class the new object should represent.
	 * @param singleton if true method getInstance() will return always the identical instance
	 */
	public ClassInfo( Class aClass, boolean singleton )
	{
		this( aClass ) ;
		this.setIsSingleton( singleton ) ;
	} // ClassInfo() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Determines if the class or interface represented by this ClassInfo object is 
	 * either the same as, or is a superclass or superinterface of, the class 
	 * or interface represented by the specified Class parameter. 
	 * It returns true if so; otherwise it returns false. 
	 * If this ClassInfo object represents a primitive type, this method returns 
	 * true if the specified Class parameter is exactly this Class object; 
	 * otherwise it returns false.
	 * 
	 * @param type the Class object to be checked
	 */
	public boolean isAssignableFrom( Class type ) 
	{
		if ( type == null )
		{
			return false ;
		}
		if ( this.getClassObject() == null )
		{
			return false ;
		}
		return this.getClassObject().isAssignableFrom( type ) ;
	} // isAssignableFrom() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Determines if the class or interface represented by the specified class parameter is 
	 * either the same as, or is a superclass or superinterface of, the class 
	 * or interface represented by this ClassInfo object. 
	 * It returns true if so; otherwise it returns false. 
	 * If specified class parameter is a primitive type, this method returns 
	 * true if this ClassInfo object rerpresents exactly the same Class object; 
	 * otherwise it returns false.
	 * 
	 * @param type the Class object to be checked
	 */
	public boolean isAssignableTo( Class type ) 
	{
		if ( type == null )
		{
			return false ;
		}
		if ( this.getClassObject() == null )
		{
			return false ;
		}
		return type.isAssignableFrom( this.getClassObject() ) ;
	} // isAssignableTo() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Determines if the specified Object is assignment-compatible with the object 
	 * represented by this ClassInfo. 
	 * This method is the dynamic equivalent of the Java language instanceof  operator. 
	 * The method returns true if the specified Object argument is non-null and can be 
	 * cast to the reference type represented by this ClassInfo object without raising 
	 * a ClassCastException. It returns false otherwise.
	 * 
	 * @param object the object to check
	 */
	public boolean isInstance( Object object ) 
	{
		if ( object == null )
		{
			return false ;
		}
		if ( this.getClassObject() == null )
		{
			return false ;
		}
		return this.getClassObject().isInstance( object ) ;
	} // isInstance() 
	
	// -------------------------------------------------------------------------
	
  /**
   * Returns the name of the class, this object represents.
   */
	public String getClassName()
	{
		return this.className() ;
	} // getClassName() 

	// -------------------------------------------------------------------------

	/**
	 * Set the name of the class this object should represent.
	 * Additionally the corresponding class object is set to null.
	 * 
	 * @param className A fully qualified class name
	 */
	public void setClassName( String className )
	{
		if ( className != null )
		{
			this.className( className ) ;
			this.classObject( null ) ;
		}
	} // setClassName() 

	// -------------------------------------------------------------------------

  /**
   * Returns the the class object.
   * If the class can't be found by the class loader, null will be returned.
   */
	public Class<T> getClassObject()
	{
		if ( this.classObject() == null )
		{
			this.initClassObject() ;
		}
		return this.classObject() ;
	} // getClassObject() 

	// -------------------------------------------------------------------------

	/**
	 * Set the class object. The name will be set automatically to the given
	 * class's name.
	 * 
	 * @param aClass A class
	 */
	public void setClassObject( Class<T> aClass )
	{
		if ( aClass != null )
		{
			this.classObject( aClass ) ;
			this.className( aClass.getName() ) ;
		}
	} // setClassObject() 

	// -------------------------------------------------------------------------

	/**
	 * Returns either a new instance of the underlying class or always the 
	 * identical instance if it is defined to be a singleton.
	 * 
	 * @see #isSingleton()
	 * @see #setIsSingleton(boolean)
	 * @return An instance of the underlying class
	 */
	public T getInstance() 
	{
		if ( this.isSingleton() )
		{
			if ( this.getSoleInstance() == null )
			{
				this.setSoleInstance( this.createInstance() ) ;
			}
			return this.getSoleInstance() ;
		}
		return this.createInstance() ;
	} // getInstance()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns a new instance of the class or null in any case of error.
	 * If more detailed information about a failed instance creation is
	 * necessary, the better method is <code>newInstance</code>.
	 * 
	 * @see #newInstance()
	 */
	public T createInstance()
	{
		try
		{
			return this.newInstance() ;
		}
		catch ( Exception ex )
		{
		}
		return null ;
	} // createInstance() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new instance of the class.
	 * 
	 * @see #createInstance()
	 * @throws ClassNotFoundException If the class represented by this object can't be found
	 * @throws InstantiationException If no none-argument constructor is available/visible
	 * @throws IllegalAccessException If for security reasons the instance creation of this class prohibited
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 */
	public T newInstance()
		throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		Class<T> aClass  ;
		aClass = this.getClassObject() ;
		if ( aClass == null )
		{
			throw new ClassNotFoundException( this.className() ) ;
		}
		return aClass.getDeclaredConstructor().newInstance() ;	
	} // newInstance() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new array of the type represented by this ClassInfo. Returns
	 * Object[0] if this ClassInfo does not represent a valid and loadable class.
	 *  
	 * @param size The number of elements the new array whould have (must not be negative)
	 * @return the new array
	 * @throws NullPointerException if the specified componentType parameter is null
	 * @throws IllegalArgumentException if componentType is Void.TYPE
	 * @throws NegativeArraySizeException if the specified length is negative
	 */
	public T[] newArray( int size ) 
	{
		if ( this.getClassObject() != null )
		{
			return (T[])Array.newInstance( this.getClassObject(), size ) ;			
		}
		return (T[])EMPTY_OBJECT_ARRAY ;
	} // newArray()
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return this.getClass().getName() + "(" + this.getClassName() + ")" ;
	} // toString() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void initClassObject()
	{
		Class aClass ;
		
		aClass = ReflectUtil.current().findClass( this.getClassName() ) ;
		if ( aClass != null )
		{
			this.classObject( aClass ) ;
		}
	} // initClassObject() 

	// -------------------------------------------------------------------------

} // class ClassInfo 
