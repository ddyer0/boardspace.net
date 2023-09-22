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
package lib;

import java.util.*;


/** a simple subclass of Observable so we can call the protected method setChanged */
public class SimpleObservable
{	
    private boolean changed = false;
    Object target = null;
    private Vector<SimpleObserver> obs =  new Vector<SimpleObserver>();
    public Object getTarget() { return(target); }

    /** Construct an Observable with zero Observers. */
    public SimpleObservable(Object t) {
    	target = t;
    }
    public SimpleObservable() {
    }
    
    /** set as changed and notify observers of change */
    public void setChanged(Object f)
    {
    	changed = true;
        notifyObservers(null,f);
    }
    public void setChanged(Object eventType,Object f)
    {  
    	changed = true;
        notifyObservers(eventType,f);
    }
    /**
     * Adds an observer to the set of observers for this object, provided
     * that it is not the same as some observer already in the set.
     * The order in which notifications will be delivered to multiple
     * observers is not specified. See the class comment.
     *
     * @param   o   an observer to be added.
     * @throws NullPointerException   if the parameter o is null.
     */
    public synchronized void addObserver(SimpleObserver o) {
        if (o == null)
            throw new NullPointerException();
        if (!obs.contains(o)) {
            obs.addElement(o);
        }
    }

    /**
     * Deletes an observer from the set of observers of this object.
     * Passing {@code null} to this method will have no effect.
     * @param   o   the observer to be deleted.
     */
    public synchronized void removeObserver(SimpleObserver o) {
        obs.removeElement(o);
    }

    /**
     * If this object has changed, as indicated by the
     * {@code hasChanged} method, then notify all of its observers
     * and then call the {@code clearChanged} method to indicate
     * that this object has no longer changed.
     * <p>
     * Each observer has its {@code update} method called with two
     * arguments: this observable object and the {@code arg} argument.
     *
     * @param   arg   any object.
     * @see     lib.SimpleObservable#clearChanged()
     * @see     lib.SimpleObservable#hasChanged()
     */
    private void notifyObservers(Object eventType,Object arg) {
        /*
         * a temporary array buffer, used as a snapshot of the state of
         * current Observers.
         */
        Object[] arrLocal;

        synchronized (this) {
            /* We don't want the Observer doing callbacks into
             * arbitrary code while holding its own Monitor.
             * The code where we extract each Observable from
             * the Vector and store the state of the Observer
             * needs synchronization, but notifying observers
             * does not (should not).  The worst result of any
             * potential race-condition here is that:
             * 1) a newly-added Observer will miss a
             *   notification in progress
             * 2) a recently unregistered Observer will be
             *   wrongly notified when it doesn't care
             */
            if (!changed)
                return;
            arrLocal = obs.toArray();
            clearChanged();
        }

        for (int i = arrLocal.length-1; i>=0; i--)
            ((SimpleObserver)arrLocal[i]).update(this, eventType, arg);
    }

    /**
     * Clears the observer list so that this object no longer has any observers.
     */
    public synchronized void deleteObservers() {
        obs.removeAllElements();
    }

 
    public synchronized void setChanged() {
        changed = true;
        notifyObservers(null,null);
    }

    /**
     * Indicates that this object has no longer changed, or that it has
     * already notified all of its observers of its most recent change,
     * so that the {@code hasChanged} method will now return {@code false}.
     * This method is called automatically by the
     * {@code notifyObservers} methods.
     *
     * @see     lib.SimpleObservable#notifyObservers()
     * @see     lib.SimpleObservable#notifyObservers(java.lang.Object)
     */
    protected synchronized void clearChanged() {
        changed = false;
    }

    /**
     * Tests if this object has changed.
     *
     * @return  {@code true} if and only if the {@code setChanged}
     *          method has been called more recently than the
     *          {@code clearChanged} method on this object;
     *          {@code false} otherwise.
     * @see     lib.SimpleObservable#clearChanged()
     * @see     lib.SimpleObservable#setChanged()
     */
    public synchronized boolean hasChanged() {
        return changed;
    }

    /**
     * Returns the number of observers of this {@code Observable} object.
     *
     * @return  the number of observers of this object.
     */
    public synchronized int countObservers() {
        return obs.size();
    }
}