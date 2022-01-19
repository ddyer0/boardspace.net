// ===========================================================================
// CONTENT  : CLASS Trigger
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 06/03/2004
// HISTORY  :
//  26/05/2002  duma  CREATED
//	06/03/2004	duma	added		-->	terminate()
//
// Copyright (c) 2002-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * A trigger is an object that runs in a separate thread and waits for 
 * a specified time until it calls a specific method of a trigger client
 * (see {@link TriggerClient}).
 * Before it actually calls the client's <i>triggerdBy()</i> method it
 * checks the client's <i>canBeTriggered()</i> method that allows it
 * or not.
 * After the client's <i>triggerdBy()</i> method was called the trigger
 * waits again until the end of its waiting period.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class Trigger implements Runnable
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private String name = null;
	/**
	 * Returns the name of the trigger
	 */
	public String getName()
	{
		return name;
	}
	protected void setName(String newValue)
	{
		name = newValue;
	}

	private TriggerClient client = null;
	protected TriggerClient getClient()
	{
		return client;
	}
	protected void setClient(TriggerClient newValue)
	{
		client = newValue;
	}

	private long interval = 300000; // milliseconds
	protected long getInterval()
	{
		return interval;
	}
	protected void setInterval(long newValue)
	{
		interval = newValue;
	}

	private boolean suspended = false;
	protected boolean getSuspended()
	{
		return suspended;
	}
	protected void setSuspended(boolean newValue)
	{
		suspended = newValue;
	}

	private boolean terminated = false;
	protected boolean getTerminated()
	{
		return terminated;
	}
	protected void setTerminated(boolean newValue)
	{
		terminated = newValue;
	}

	// =========================================================================
	// CLASS METHODS
	// =========================================================================

	/**
	 * Launch a new trigger with the given name for the given client, that
	 * calls the <i>triggeredBy</i> method of the client every <i>intervalInMs</i>
	 * milliseconds.<br>
	 * The ne trigger will be created and immediately started in a separate 
	 * thread.
	 * 
	 * @param name The name of the trigger (must not be null)
	 * @param client The client that gets triggered (must not be null)
	 * @param intervalInMs The interval after that the client regularily gets triggered (must be greater than 0)
	 * @return The newly created trigger
	 * @throws IllegalArgumentException if any of the given arguments is null
	 * @see TriggerClient
	 */
	public static Trigger launch(
		String name,
		TriggerClient client,
		long intervalInMs)
	{
		String invalidArg	= null ;
		Trigger trigger		= null ;
		Thread thread			= null ;
		
		if (name == null)
			invalidArg = "name" ;
		else if (client == null)
			invalidArg = "client" ;
		else if (intervalInMs <= 0)
			invalidArg = "intervalInMs" ;
			
		if ( invalidArg != null )	
		{
			throw new IllegalArgumentException(
				Trigger.class.getName() + ".launch() -> " + invalidArg ) ;
		}
		
		trigger = new Trigger( name, client, intervalInMs ) ;
		thread = new Thread( trigger ) ;
		thread.start() ;

		return trigger ;
	} // launch()

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	protected Trigger(String name, TriggerClient client, long intervalInMs)
	{
		super();
		this.setName(name);
		this.setClient(client);
		this.setInterval(intervalInMs);
	} // Trigger()

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Terminates the trigger. That means the thread the trigger used internally
	 * will be stopped when it awakes the next time (depends on interval)
	 */
	public void terminate()
	{
		this.setTerminated( true ) ;
	} // terminate()

	// -------------------------------------------------------------------------

	/**
	 * Implementation of Runnable. <p>
	 * Must not be called directly!
	 */
	public void run()
	{
		while (!this.isTerminated())
		{
			try
			{
				Thread.sleep( this.getInterval() );
				if ( ( ! this.isTerminated() ) && ( ! this.isSuspended() ) )
				{
					if ( this.canBeTriggered() )
					{
						this.trigger() ;
					}
				}
			}
			catch (InterruptedException ex)
			{
				this.setTerminated(true);
			}
		}
	} // run()

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================

	protected boolean canBeTriggered()
	{
		return this.getClient().canBeTriggeredBy( this ) ;
	} // canBeTriggered()

	// -------------------------------------------------------------------------

	protected void trigger()
	{
		boolean cont ;
		
		cont = this.getClient().triggeredBy( this ) ;
		if ( ! cont )
			this.setSuspended( true ) ;
	} // trigger()

	// -------------------------------------------------------------------------

	protected boolean isTerminated()
	{
		return this.getTerminated();
	} // isTerminated()

	// -------------------------------------------------------------------------

	protected boolean isSuspended()
	{
		return this.getSuspended();
	} // isSuspended()

	// -------------------------------------------------------------------------

} // class Trigger