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
