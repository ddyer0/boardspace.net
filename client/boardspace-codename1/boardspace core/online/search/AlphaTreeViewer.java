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
package online.search;

import lib.G;
import lib.InspectorInterface;
import lib.ShellProtocol;

public class AlphaTreeViewer extends TreeViewer 
{

	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;
	Search_Driver search = null;
	
	public void setTree(TreeProviderProtocol s)
	{	if(s instanceof Search_Driver)
		{
		search = (Search_Driver)s;

		repaint();
		}
	}
	public void startInspector()
	{
		InspectorInterface inspect = (InspectorInterface)G.MakeInstance("lib.InspectorBridge");
		inspect.view(search);
	}
	public void startShell()
	    {	
	    	ShellProtocol shell = (ShellProtocol)G.MakeInstance("lib.ShellBridge");
	    	shell.startShell("viewer",this,"searcher",search,"out",System.out);
	    	G.setPrinter(shell);
	    }

}
