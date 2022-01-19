package lib;

/**
 * this is the standard "Error" subclass to use.  It allows you to add additional
 * information to the error object as part of handling the error.  Used to append
 * context information before reporting the problem.
 * 
 * @author ddyer
 *
 */
public class ErrorX extends Error {
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	String extraInfo="";
	public void addExtraInfo(String more) { extraInfo += more + "\n"; }
	public ErrorX(String m) { super(m); }
	public ErrorX(Throwable m) { super(m.toString()); }
	
	public void printStackTrace()
	{
		super.printStackTrace();
		if(!"".equals(extraInfo)) { G.print(extraInfo); }
	}
	// note that because of the way codenameone structures printStackTrace,
	// if we implement it here a stack overflow will result
	//public void printStackTraceX(PrintStream s)
	//{	super.printStackTrace(s);
	//	if(!"".equals(extraInfo)) { s.println(extraInfo); }
	//}
}
