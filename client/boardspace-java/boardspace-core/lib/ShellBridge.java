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

import javax.swing.JFrame;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;
/**
 * This class bridges between a general application and beanshell.  It's intended to
 * be instantiated by class.forName(ShellBridge) so the main project doesn't have
 * and explicit dependency on beanshell, and can be deployed without beanshell, which
 * is only intended for debugging.
 * @author ddyer
 *
 */
public class ShellBridge implements ShellProtocol
{
    JFrame consoleFrame = null;
    JConsole console = null;
    Interpreter shell = null;
    
    // constructor
    public ShellBridge() {}
    
    /**
     * this allows the client to print into the bean shell console window.
     */
    public void print(Object... o) 
    { if(o!=null) { for(int i=0;i<o.length;i++) { shell.print(o[i]); } }
    }
    /**
     * this allows the client to print into the bean shell console window.
     */
    public void println(Object... o)
    { if(o!=null) { for(int i=0;i<o.length;i++) { shell.print(o[i]); } }
      shell.println("");
    }
    /**
     * start the shell, and provide a list of useful starting points.
     * @param roots is a list of  name , value pairs
     */
	public void startShell(Object... roots)
	{
	console = new JConsole();  // Construct an interpreter
	console.setSize(500,500);
	consoleFrame = new JFrame("console frame");
	consoleFrame.setSize(600,600);
	consoleFrame.add(console);
	shell = new Interpreter( console );
   	new Thread( shell ,"shell").start(); // start a thread to call the run() method
	console.setVisible(true);
	consoleFrame.setVisible(true);
	
//	Inspector.inspect(this);
 	
	try
	{
	String name = "root";
	int seq = 0;
	shell.setShowResults(true);
	if(roots!=null)
	{
	for(int idx = 0;idx<roots.length;idx++)
		{ 
		Object root = roots[idx];
		if(root  instanceof String)
		{
			name = (String)root;
			seq = 0;
		}
		else
		{
		String xname = name + ((seq==0)?"":seq);
		shell.set(xname,root);
		shell.print(xname+"=");
		shell.println(root);
		seq++;
		}}}
	shell.eval("setAccessibility(true)");

	}
	catch (EvalError err)
	{
		System.out.println("Bean shell err: "+err);
	}
	}
}
