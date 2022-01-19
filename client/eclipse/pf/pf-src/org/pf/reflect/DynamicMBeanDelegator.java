// ===========================================================================
// CONTENT  : CLASS DynamicMBeanDelegator
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 06/05/2012
// HISTORY  :
//  06/05/2012  mdu  CREATED
//
// Copyright (c) 2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.reflect ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;

/**
 * An generic reflection based implementation for applying a DynamicMBean
 * interface on a another object that actually does not need to implement
 * the DynamicMBean interface.
 * <p>
 * The dynamic method invocations and attribute access will be delegated on
 * the internally held receiver object.
 * <p>
 * The generic type TRECEIVER represents the type of the internal object that
 * is the target for all method invocations and attribute access.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class DynamicMBeanDelegator<TRECEIVER> //implements DynamicMBean
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final ReflectUtil RU = ReflectUtil.current();

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private TRECEIVER receiver = null ;
  public TRECEIVER getReceiver() { return receiver ; }
  public void setReceiver( TRECEIVER newValue ) { receiver = newValue ; }
  
  private MBeanInfo managedBeanInfo = null ;
  protected MBeanInfo getManagedBeanInfo() { return managedBeanInfo ; }
  public void setManagedBeanInfo( MBeanInfo newValue ) { managedBeanInfo = newValue ; }
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with the target object.
   */
  public DynamicMBeanDelegator(TRECEIVER receiver)
  {
    super() ;
    this.setReceiver(receiver);
  } // DynamicMBeanDelegator()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Executes the methods that are supported via JMX.
   */
	public Object invoke(String methodName, Object[] params, String[] signature)
		throws MBeanException, javax.management.ReflectionException
	{
		Class[] paramTypes = null;
		Object result;

		try
		{
			if ((signature != null) && (signature.length > 0))
			{
				paramTypes = RU.findClasses(signature);
			}
			result = Dynamic.perform(this.getReceiver(), methodName, params, paramTypes);
		}
		catch (Exception e)
		{
			throw new javax.management.ReflectionException(e, "Failed to dynamically invoke MBean method: " + methodName);
		}
		if (result instanceof Exception)
		{
			throw new MBeanException((Exception)result, "MBean method invocation returned exception.");
		}
		return result;
	} // invoke() 
  
  // -------------------------------------------------------------------------
  
	/**
	 * Returns the value of the attribute with the given name.
	 * 
	 * @param attrName The name of the attribute to be returned
	 * @throws AttributeNotFoundException if the attribute with the given name cannot be found.
	 */
	public Object getAttribute(String attrName) throws AttributeNotFoundException, MBeanException, ReflectionException
	{
		try
		{
			return RU.getValueOf(this.getReceiver(), attrName);
		}
		catch (NoSuchFieldException ex)
		{
			throw new AttributeNotFoundException(attrName);
		}
	} // getAttribute() 

	// -------------------------------------------------------------------------

	/**
	 * Sets the specified attribute.
	 * 
	 * @param attr The attribute with name and value.
	 * @throws AttributeNotFoundException if the attribute with the given name cannot be found.
	 */
	public void setAttribute(Attribute attr)
		throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
	{
		try
		{
			RU.setValueOf(this.getReceiver(), attr.getName(), attr.getValue());
		}
		catch (NoSuchFieldException ex)
		{
			throw new AttributeNotFoundException(attr.getName());
		}
	} // setAttribute() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all the attributes with the specified names.
	 * 
	 * @throws IllegalArgument In any case of problem setting one of the attributes.
	 * The original exception will be available via getCause(). 
	 */
	public AttributeList getAttributes(String[] attrNames)
	{
		AttributeList attrList;
		Attribute attr;
		Object value;
		
		attrList = new AttributeList(attrNames.length);
		for (String attrName : attrNames)
		{
			try
			{
				value = this.getAttribute(attrName);
			}
			catch (Throwable ex)
			{
				throw new IllegalArgumentException(ex);
			}
			attr = new Attribute(attrName, value);
			attrList.add(attr);
		}
		return attrList;
	} // getAttributes() 

	// -------------------------------------------------------------------------

	/**
	 * Sets all specified attributes.
	 * 
	 * @throws IllegalArgument In any case of problem setting one of the attributes.
	 * The original exception will be available via getCause(). 
	 */
	/*
	public AttributeList setAttributes(AttributeList attributes)
	{
		Attribute attr;
		
		for (Object object : attributes)
		{
			if (object instanceof Attribute)
			{
				attr = (Attribute)object;
				try
				{
					this.setAttribute(attr);
				}
				catch (Throwable ex)
				{
					throw new IllegalArgumentException(ex);
				}
			}
		}
		return attributes;
	} // setAttributes() 
*/
	// -------------------------------------------------------------------------

	/**
	 * Returns the meta data describing the supported attributes and methods.
	 * Subclasses may override this method if necessary, otherwise the 
	 * {@link #setManagedBeanInfo(MBeanInfo)} method can be used to set the
	 * meta data.
	 */
	public MBeanInfo getMBeanInfo()
	{
		return this.getManagedBeanInfo();
	} // getMBeanInfo() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  
} // class DynamicMBeanDelegator
