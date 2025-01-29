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
package online.game.sgf;

import java.io.PrintStream;

import lib.G;
import lib.OStack;
import lib.StackIterator;
import online.game.sgf.export.sgf_names;

class NodeStack extends OStack<sgf_node> implements StackIterator<sgf_node>
{
	public sgf_node[] newComponentArray(int sz) { return(new sgf_node[sz]); }

}
/*
 $Id: sgf_node.java,v 1.1.1.1.2.11.2.17 2025/01/29 00:16:47 ddyer Exp $

 $Log: sgf_node.java,v $
 Revision 1.1.1.1.2.11.2.17  2025/01/29 00:16:47  ddyer
 clean up and improve the StackIterator class

 Revision 1.1.1.1.2.18  2023/09/22 03:57:33  ddyer
 add copyright and license

 Revision 1.1.1.1.2.17  2022/01/08 02:32:04  ddyer
 clean up some error messages

 Revision 1.1.1.1.2.16  2021/05/26 19:06:07  ddyer
 *** empty log message ***

 Revision 1.1.1.1.2.15  2020/07/20 21:47:09  ddyer
 massive dogwash to reorganize "commonMove"

 Revision 1.1.1.1.2.14  2020/06/17 23:52:46  ddyer
 change sgf_node to use stackiterator instead of vector
 change extendedhashtable to use treemap instead of hashtable
 these are to help vitiulture work better on ipads.

 Revision 1.1.1.1.2.13  2016/10/25 21:42:27  ddyer
 tune up the javadoc

 Revision 1.1.1.1.2.12  2015/12/08 20:53:53  ddyer
 replace PrintWriter with PrintStream, for compatability with CodenameOne

 Revision 1.1.1.1.2.11  2015/04/10 05:41:00  ddyer
 *** empty log message ***

 Revision 1.1.1.1.2.10  2015/03/22 06:15:18  ddyer
 tweak forward/backward movement

 Revision 1.1.1.1.2.9  2011/02/22 21:04:11  ddyer
 use generic types and remove unnecessary casts

 Revision 1.1.1.1.2.8  2009/09/13 20:05:44  ddyer
 *** empty log message ***

 Revision 1.1.1.1.2.7  2009/03/07 22:09:21  ddyer
 *** empty log message ***

 Revision 1.1.1.1.2.6  2005/07/21 22:38:51  ddyer
 reformat with jalopy

 Revision 1.1.1.1.2.5  2005/03/27 08:26:54  ddyer
 *** empty log message ***

 Revision 1.1.1.1.2.4  2005/03/03 05:33:25  ddyer
 *** empty log message ***

 Revision 1.1.1.1.2.3  2005/03/02 22:46:15  ddyer
 after eclipse compiler lint cleanup

 Revision 1.1.1.1.2.2  2005/03/02 02:10:50  ddyer
 *** empty log message ***

 Revision 1.1.1.1  2002/10/28 04:38:59  ddyer
 online tantrix java

 Revision 1.9  1996/09/22 21:21:27  ddyer
 web release 9/15/96

 Revision 1.8  1996/09/18 01:19:39  ddyer
 Added inline input

 Revision 1.7  1996/09/16 00:44:28  ddyer
 some tweaks for loajava

 Revision 1.6  1996/09/07 01:24:57  ddyer
 $1

 Revision 1.5  1996/09/07 01:22:23  ddyer
 $1

*/

/** sgf_node contains a list of properties which are part of the node, and a second
list of auxiliary properties which are for the client application to use any way
it wants; for example, to keep track of active information as the node is used to
display a game. */
public class sgf_node implements sgf_names, StackIterator<sgf_node>
{
    private StackIterator<sgf_node> next = null;
    /** the previous node to this node, in play order */
    public sgf_node prev;
    /** properties that are printed by {@link #sgf_print} - ie those that are permanant */
    public sgf_property properties; // these are printed by sgf_print
    /** ephemeral properties of this node, for temporary use */
    public sgf_property local_properties; // these are not printed by sgf_print

    /** default constructor */
    public sgf_node()
    {
    }
/**
 * constructor with a property list
 * @param prop
 */
    public sgf_node(sgf_property prop)
    {
        this.properties = prop;
    }


    /**
     * get the first successor of this node
     * @return a sgf_node
     */
    public sgf_node firstElement()
    {
        return ((next == null) ? null : next.elementAt(0));
    }

    /**
     * get the number of succesors of this node
     * @return an integer
     */
    public int nElements() { return((next==null) ? 0 : next.size()); }
    public sgf_node getSuccessor(int n) { return(next.elementAt(n)); }
    /**
     * get the n'th successor of this node
     * @param n
     * @return a sgf_node
     */
    public sgf_node nThElement(int n) { return((next==null) ? null : next.elementAt(n)); }

    /**
     *  add element e, null is ok (not added, just ok), and back link to this node 
     *  where is {@link online.game.sgf.export.sgf_names.Where}
     */
    public void addElement(sgf_node e, Where where)
    {
        if (e != null)
        {
            if (next == null)
            {
                next = null;
            }
            switch(where)
            {
            case atEnd:
            	{
                if(next==null) { next = e; } else { next = next.push(e); }
            	}
            	break;
            case asOnly:
            	{
                next = e;
            	}
            	break;
            case atBeginning:
            	{
            	if(next==null) { next = e; }
            	else { 
            		StackIterator<sgf_node> on = next;
            		next = new NodeStack();
            		next.push(e);
            		for(int i=0;i<on.size();i++) { next.push(on.elementAt(i)); }
            	}}
            	break;
            default: G.Error("Not expecting where=%s",where);
            }
            e.prev = this;
        }
    }
    /** append a successor to this node */
    public void addElement(sgf_node e)
    {
        addElement(e, Where.atEnd);
    }
/**
 * get the property with name
 * @param propname
 * @return a String
 */
    public String get_property(String propname)
    {
        sgf_property prop = sgf_property.get_sgf_property(properties, propname);

        return ((String) ((prop == null) ? null : prop.getValue()));
    }

    /** set the property value of the property with the specified name.
    Setting a value of null removes the property
    this also guaranteed to promote the property to the head of the list
    @param propname
    @param propval
    */
    public void set_property(String propname, String propval)
    {
        sgf_property old = sgf_property.get_sgf_property(properties, propname);
        if(old!=null) { properties = sgf_property.remove_sgf_property(properties, old); }
        if(propval!=null) { properties = new sgf_property(propname, propval, properties); }
    }
/**
 * get the local property with name
 * @param propname
 * @return an Object
 */
    public Object get_local_property(String propname)
    {
        sgf_property prop = sgf_property.get_sgf_property(local_properties,
                propname);

        return ((prop == null) ? null : prop.getValue());
    }
    /** set the property value of the property with the specified name.
    Setting a value of null removes the property
    this also guaranteed to promote the property to the head of the list
    */
    public void set_local_property(String propname, Object propval)
    {
        sgf_property old = sgf_property.get_sgf_property(local_properties,
                propname);

        if (old == null)
        {
            if (propval != null)
            {
                local_properties = new sgf_property(propname, propval,
                        local_properties);
            }
        }
        else
        {
            if (propval == null)
            {
                local_properties = sgf_property.remove_sgf_property(local_properties,
                        old);
            }
            else
            {
                old.setValue(propval);
            }
        }
    }

    /**
     * print for sgf file
     * 
     * @param out
     */
    public void sgf_print(PrintStream out)
    {
        sgf_property prop = properties;
        out.print(";");

        while (prop != null)
        {
            prop.sgf_print(out);
            prop = prop.next;
        }
    }

	public StackIterator<sgf_node> push(sgf_node item) {
		NodeStack sn = new NodeStack();
		sn.push(this);
		sn.push(item);
		return(sn);
	}
	public StackIterator<sgf_node> insertElementAt(sgf_node item,int at)
	{
		NodeStack sn = new NodeStack();
		sn.push(this);
		return(sn.insertElementAt(item, at));
	}

}
