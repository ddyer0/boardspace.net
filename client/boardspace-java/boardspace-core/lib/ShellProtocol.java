package lib;
/**
 * this class is the generic interface for an object you can print to.
 * launch a console window and to print things into it.
 * 
 * @author ddyer
 *
 */
public interface ShellProtocol 
{	/** start a shell in a separate window, with a list of name value pairs as
		known symbols in the console.
 	*/
	public void startShell(Object... args);
	/**
	 * print objects into the console window
	 * @param o
	 */
	public void print(Object... o);
	/**
	 * print objects into the console window
	 * @param o
	 */
	public void println(Object... o);
}
