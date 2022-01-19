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
    { for(int i=0;i<o.length;i++) { shell.print(o[i]); }
    }
    /**
     * this allows the client to print into the bean shell console window.
     */
    public void println(Object... o)
    { for(int i=0;i<o.length;i++) { shell.print(o[i]); }
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
		}}
	shell.eval("setAccessibility(true)");

	}
	catch (EvalError err)
	{
		System.out.println("Bean shell err: "+err);
	}
	}
}
