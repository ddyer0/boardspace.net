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

import java.net.URL;

import online.game.sgf.export.sgf_names;

import java.io.*;



/*
 $Id: sgf_game.java,v 1.1.1.1.2.19 2023/09/22 03:57:33 ddyer Exp $

 $Log: sgf_game.java,v $
 Revision 1.1.1.1.2.19  2023/09/22 03:57:33  ddyer
 add copyright and license

 Revision 1.1.1.1.2.18  2021/05/26 19:06:06  ddyer
 *** empty log message ***

 Revision 1.1.1.1.2.17  2020/12/19 21:39:09  ddyer
 Q/A to make all archived games replay correctly

 Revision 1.1.1.1.2.16  2020/06/17 23:52:46  ddyer
 change sgf_node to use stackiterator instead of vector
 change extendedhashtable to use treemap instead of hashtable
 these are to help vitiulture work better on ipads.

 Revision 1.1.1.1.2.15  2016/09/15 17:49:21  ddyer
 record the file name as game name as a starting value, to help track
 where replay problems come from.

 Revision 1.1.1.1.2.14  2016/03/29 19:34:14  ddyer
 collate with codename1 version

 Revision 1.1.1.1.2.13  2015/12/08 20:53:53  ddyer
 replace PrintWriter with PrintStream, for compatability with CodenameOne

 Revision 1.1.1.1.2.12  2015/11/01 21:08:23  ddyer
 allow loading zip files with extension "zzz" with delayed loading of contents.
 load using utf-8 formatting.

 Revision 1.1.1.1.2.11  2014/07/09 17:46:01  ddyer
 *** empty log message ***

 Revision 1.1.1.1.2.10  2012/07/28 01:49:17  ddyer
 *** empty log message ***

 Revision 1.1.1.1.2.9  2011/02/22 21:04:11  ddyer
 use generic types and remove unnecessary casts

 Revision 1.1.1.1.2.8  2009/03/07 22:09:21  ddyer
 *** empty log message ***

 Revision 1.1.1.1.2.7  2005/07/22 02:02:31  ddyer
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

 Revision 1.12  1996/09/22 21:21:27  ddyer
 web release 9/15/96

 Revision 1.11  1996/09/18 01:19:39  ddyer
 Added inline input

 Revision 1.10  1996/09/16 00:44:28  ddyer
 some tweaks for loajava

 Revision 1.9  1996/09/10 02:04:16  ddyer
 fixed close, changed property value to object

 Revision 1.8  1996/09/07 01:31:52  ddyer
 added log

 Revision 1.9  1999/12/17 12:01:33  tom francis
 printing of variations
*/

public class sgf_game extends Object implements sgf_names
{
	static URL zipFile = null;
    public String source_file;
    public int sequence;
    public sgf_node root;
    public void setRoot(sgf_node n) { root=n; }
    public sgf_node getRoot() 
    {
    	if(root==null) 
    		{ 
    		  if(zipFile!=null)
    		  {
    			  sgf_game[] newgame = sgf_reader.parse_sgf_file(zipFile,source_file,null);
    			  root = newgame[0].root;
    		  }
    		  if(root==null)
    		  {
    			  root=new sgf_node();
    		  }
    		}
    	return(root);
    }
    boolean strip = false;
    public String toString() { return("<sgf_game "+short_name()+">"); }
    // constructor
    public sgf_game(URL zip,String file)
    {	zipFile = zip;
    	source_file = file;
    	root = new sgf_node(new sgf_property(fileformat_property,
                sgf_file_format));
    	root.set_property(gamename_property,source_file);
    }
    // constructor
    public sgf_game(sgf_node ro)
    {
    	root = ro;
    }
    // constructor
    public sgf_game()
    {
        root = new sgf_node(new sgf_property(fileformat_property,
                    sgf_file_format));
     }

    public void sgf_print(PrintStream out, boolean stripped)
        throws IOException
    {
        strip = stripped;
        sgf_print_tree(out, root, 0);
    }

    public void sgf_print(PrintStream out) throws IOException
    {
        sgf_print_tree(out, root, 0);
    }

    public String short_name()
    {	String gn =  root.get_property(gamename_property);
        return ((root==null || gn==null || "".equals(gn)) ? source_file : gn);
    }
    
    public void set_short_name(String val)
    {
        root.set_property(gamename_property,val);
    }
    private void sgf_print_tree_stripped(PrintStream out, sgf_node rootnode,
        int level)
    {
        rootnode.sgf_print(out);

        while (rootnode.size() >= 1)
        {
            rootnode = rootnode.firstElement();

            //out.print(Character.LINE_SEPARATOR); 
            rootnode.sgf_print(out);
        }
    }

    private void sgf_print_tree_whole(PrintStream out, sgf_node rootnode, int level)
        throws IOException
    {
        rootnode.sgf_print(out);

        while (rootnode!=null && rootnode.nElements() == 1)
        {
            rootnode = rootnode.firstElement();

            //out.print(Character.LINE_SEPARATOR); 
            if(rootnode!=null) { rootnode.sgf_print(out); }
        }

        if (rootnode!=null && (rootnode.nElements() > 1))
        {
            out.println(" ");

            int nSuccessors = rootnode.nElements();
            for(int i=0;i<nSuccessors;i++)
            {
                sgf_node node = rootnode.getSuccessor(i);
                sgf_print_tree(out, node, level + 1);
            }

            int templevel = level;

            while (templevel > 0)
            {
                out.print(" ");
                templevel--;
            }
        }
    }

    private void sgf_print_tree(PrintStream out, sgf_node rootnode, int level)
        throws IOException
    {
        //out.print(Character.LINE_SEPARATOR); 
        int templevel = level;

        while (templevel > 0)
        {
            out.print(" ");
            templevel--;
        }

        out.print("(");

        if (strip)
        {
            sgf_print_tree_stripped(out, rootnode, level);
        }
        else
        {
            sgf_print_tree_whole(out, rootnode, level);
        }

        out.println(")");
    }
}
